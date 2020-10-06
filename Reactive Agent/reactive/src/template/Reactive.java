package template;

import java.util.Random;
import java.util.Set;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;
import template.RoadAction.RoadActionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Reactive implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	
	private static Set<RoadAction> possibleActions = new HashSet<RoadAction>(); //set of all possible cities to go to
	private static Set<State> possibleStates = new HashSet<State>();
	private List<City> allCities = new ArrayList<City>();
	
	// Q-table
	private Map<State, HashMap<RoadAction, Double>> qTable = new HashMap<State, HashMap<RoadAction, Double>>();
	// Value table
	private HashMap<State, Double> vTable = new HashMap<State, Double>();
	// Best action
    private HashMap<State, RoadAction> bestActions = new HashMap<State, RoadAction>();
	// Reward table
	private Map<State, HashMap<RoadAction, Double>> rTable = new HashMap<State, HashMap<RoadAction, Double>>();
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);
		
		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;
		this.allCities = topology.cities();
		
		//This should be changed based on the action/state implementation
		initPossibleActions(topology.cities());
		initPossibleStates(topology.cities());
		
		//Populate the tables
		initQTable();
		initVTable();
		initBestActionsTable();
		initRTable(td, agent.vehicles().iterator().next());

		reinforcementLearningAlgorithm(td);
	}
	
	@Override
	public Action act(Vehicle vehicle, Task availableTask) {				
		Action action;
		
		if (vehicle.name().equals("Intelligent Vehicle")) {
			action = intelligentAgentAct(vehicle, availableTask);
		} else if (vehicle.name().equals("Dummy Vehicle")) {
			action = dummyAgentAct(vehicle, availableTask);
		} else {
			action = randomAgentAct(vehicle, availableTask);
		}
				
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
	
	/*
	 * Reinforcement learning algorithm where the best action for a given state is being learnt
	 */
	public void reinforcementLearningAlgorithm(TaskDistribution td) {
		boolean hasConverged = false;
		while (!hasConverged) {
			HashMap<State, Double> previousVTable = new HashMap<State, Double> (vTable);

			for (State state : qTable.keySet()) {
				for (RoadAction action : qTable.get(state).keySet()) {		
					if (rTable.containsKey(state)) {
						if (rTable.get(state).containsKey(action)) {
							double value = rTable.get(state).get(action) + discountedSum(td, state, action);
							qTable.get(state).put(action, value);
						}
					}
				}
				HashMap<RoadAction, Double> values = qTable.get(state);
				RoadAction bestAction = null;
				double bestValue = 0.0;
				for (Map.Entry<RoadAction, Double> entry : values.entrySet()) {
					if (entry.getValue() >= bestValue) {
						bestValue = entry.getValue();
						bestAction = entry.getKey();
					}
				}
				vTable.put(state, bestValue);
				bestActions.put(state, bestAction);
			}
			if (converged(previousVTable, vTable)) {
				hasConverged = true;
			}
		}
	}
	
	/*
	 * Calculates and returns the transition probability for a given start state s1, action, and end state s2
	 */
	public double transitionProbability(TaskDistribution td, State s1, RoadAction action, State s2) {
		double probability = 0.0;
		//System.out.println("Action = " + action.getActionType() + " :: Curr = " + s1.getCurrentCity() + " :: Dest = " + s1.getDestinationCity());

		if (action.getActionType() == RoadActionType.MOVE) {
			// check if the action is legal
			if (s1.getDestinationCity() == null && action.getNextCity() == s2.getCurrentCity() && s1.getCurrentCity().neighbors().contains(action.getNextCity())) {
				double temp = this.highestTaskPotentialCurrent(action.getNextCity(), td);
				probability = this.highestTaskPotentialNeighbour(s1.getCurrentCity().neighbors(), td);
			}
		} else if (action.getActionType() == RoadActionType.PICKUP) {
			// check if pickup action is legal
			if (s1.getDestinationCity() != null && action.getNextCity() == s2.getCurrentCity()) {
				probability = td.probability(s1.getCurrentCity(), action.getNextCity());
			}
		}

		return probability;
	}
	
	/*
	 * Calculates the discounted sum given a current state and an action
	 */
	public double discountedSum(TaskDistribution td, State currentState, RoadAction action) {
		double sum = 0.0;
		for (State nextState : this.possibleStates) {
			double tp = transitionProbability(td, currentState, action, nextState);
			sum += tp * vTable.get(nextState);
		}
		
		sum = this.pPickup * sum;
		return sum;
	}
	
	/*
	 * Implementation of the "good enough" part of the algorithm
	 */
	public boolean converged(HashMap<State, Double> previousVTable, HashMap<State, Double> currentVTable) {
		double max = 0.0;
		for (State state : previousVTable.keySet()) {
			double difference = Math.abs(previousVTable.get(state) - currentVTable.get(state));
			if (difference > max) {
				max = difference;
			}
		}
		
		return max < 0.001;
	}
	
	/*
	 * Intelligent agent's act - decides on an action depending on the state-based best action learnt offline
	 */
	private Action intelligentAgentAct(Vehicle vehicle, Task availableTask) {
		Action action;
		
		State currentState;
		if (availableTask != null) {
			currentState = new State(vehicle.getCurrentCity(), availableTask.deliveryCity);
		} else {
			currentState = new State(vehicle.getCurrentCity());			
		}
		
		RoadAction bestAction = bestActions.get(currentState);
		
		if(bestAction.getActionType() == RoadActionType.PICKUP) {
			currentState.setDestinationCity(availableTask.deliveryCity);
			action = new Pickup(availableTask);
		} else {
			action = new Move(bestAction.getNextCity());	
		}
		
		return action;
	}
	
	/*
	 * Dummy agent's act - if there is a task available, pick it up and deliver it; otherwise choose a 
	 * random neighboring city and move to it.
	 */
	private Action dummyAgentAct(Vehicle vehicle, Task availableTask) {
		Action action;
		
		State currentState;
		if (availableTask != null) {
			currentState = new State(vehicle.getCurrentCity(), availableTask.deliveryCity);
			action = new Pickup(availableTask);
		} else {
			currentState = new State(vehicle.getCurrentCity());		
			List<City> neighbors = vehicle.getCurrentCity().neighbors();
		    Random rand = new Random(); 
		    int randomNeighborIndex = rand.nextInt(neighbors.size()); 
			action = new Move(neighbors.get(randomNeighborIndex));
		}
		
		return action;
	}
	
	/*
	 * Random agent's act
	 */
	private Action randomAgentAct(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}
		return action;
	}
	
	// Functions to initialize tables
	private void initQTable() {
		qTable = new HashMap<State, HashMap<RoadAction, Double>>();
		for (State state : this.possibleStates) {
			qTable.put(state, new HashMap<RoadAction, Double>());
			for (RoadAction action : this.possibleActions) {
				qTable.get(state).put(action, 0.0);
			}
		}
	}
	
	// Initialize V-table with random values between 0.0 and 1.0
	private void initVTable() {
	    Random randomno = new Random();
		vTable = new HashMap<State, Double>();
		for (State state : this.possibleStates) {
			vTable.put(state, randomno.nextDouble());
		}
	}
	
	private void initBestActionsTable() {
		bestActions = new HashMap<State, RoadAction>();
		for (State state : this.possibleStates) {
			bestActions.put(state, null);
		}
	}
	
	/*
	 * Initialize the reward table based on the task distribution probabilities
	 */
	private void initRTable(TaskDistribution td, Vehicle v) {
		//Initialize R table
		for (State state : this.possibleStates) {
			HashMap<RoadAction, Double> stateRewards = new HashMap<RoadAction, Double>();
			
			for (RoadAction action : this.possibleActions) {
				double reward = 0.0;
				City currentCity = state.getCurrentCity();
				City actionNextCity = action.getNextCity();
				if (action.getActionType() == RoadActionType.MOVE && state.getDestinationCity() == null) {
					if (actionNextCity != null && currentCity.hasNeighbor(actionNextCity)) {
						// Reward is negative - only cost for moving to next city
						reward -= currentCity.distanceTo(actionNextCity) * v.costPerKm();
						stateRewards.put(action, reward);
					}
				} else if (action.getActionType() == RoadActionType.PICKUP && state.getDestinationCity() != null) {
					if (action.getNextCity() == state.getDestinationCity()) {
						// Only allowed to pickup if we do not already have a destination city						
						reward += td.reward(currentCity, state.getDestinationCity()) - currentCity.distanceTo(state.getDestinationCity());
						stateRewards.put(action, reward);
					}		
				}
			}
			this.rTable.put(state, stateRewards);
		}
	}
	
	// HELPER FUNCTIONS
	private void initPossibleActions(List<City> cities) {
		for (City city : cities) {
			this.possibleActions.add(new RoadAction(city, RoadActionType.MOVE));
			this.possibleActions.add(new RoadAction(city, RoadActionType.PICKUP));
		}
	}
	
	private void initPossibleStates(List<City> cities) {
		for (City city1 : cities) {
			for (City city2: cities) {
				if(city1 == city2) {
					this.possibleStates.add(new State(city1));
				} else {
					this.possibleStates.add(new State(city1, city2));
				}
			}
		}
	}
	
	/*
	 * Given a list of neighboring cities and the task probability distribution, return the maximum
	 * probability 
	 */
	private double highestTaskPotentialNeighbour(List<City> neighbors, TaskDistribution td) {
		double max = 0.0;
		for (City n : neighbors) {
			for (City c : this.allCities) {
				if (n != c) {
					max = Math.max(max, td.probability(n, c));
				}
			}
		}	
		return max;
	}
	
	private double highestTaskPotentialCurrent(City current, TaskDistribution td) {
		double max = 0.0;
		for (City c : this.allCities) {
			if (current != c) {
				max = Math.max(max, td.probability(current, c));
			}
		}
		
		return max;
	}
}

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
	//private int costPerKm;
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
	// A mapping of probabilities that there is a task from a given city
	private Map<City, Double> totalTaskProbabilities;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);
		
		// ??? Can we do that instead of having to pass vehicle in the initRTable() function???
		// Integer costPerKm = agent.readProperty("cost-per-km", Integer.class, 5);
		
		this.random = new Random();
		this.pPickup = discount;
		//this.costPerKm = costPerKm;
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
				
		State currentState = new State(vehicle.getCurrentCity());
		RoadAction bestAction = bestActions.get(currentState);
		int counter = 0;
		for (Map.Entry<State, RoadAction> entry : bestActions.entrySet()) {
			RoadAction a = entry.getValue();
			if (a == null) {
				counter++;
			}
		}
		System.out.println("Counter is = " + counter);
		
		System.out.println("Best action's next city = " + bestAction.getNextCity());
		if(availableTask != null && bestAction.getActionType() == RoadActionType.PICKUP) {
			currentState.setDestinationCity(availableTask.deliveryCity);
		}
		
		if (currentState.getTask()) {
			action = new Pickup(availableTask);
		} else {
			action = new Move(bestAction.getNextCity());
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
	
	public void reinforcementLearningAlgorithm(TaskDistribution td) {
		boolean not_converged = true;
		while (not_converged) {
			HashMap<State, Double> previous_V_table = new HashMap<State, Double> (vTable);

			for (State state : qTable.keySet()) {
				for (RoadAction action : qTable.get(state).keySet()) {		
					if (rTable.containsKey(state)) {
						if (rTable.get(state).containsKey(action)) {
							double value = rTable.get(state).get(action) + discountedSum(td, state, action);
							qTable.get(state).put(action, value);
						}
					}
				}
				//Find best action (java 8 implementation)
				// RoadAction bestAction = qTable.get(state).entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
				HashMap<RoadAction, Double> values = qTable.get(state);
				RoadAction bestAction = null;
				double bestValue = 0.0;
				for (Map.Entry<RoadAction, Double> entry : values.entrySet()) {
					if (entry.getValue() > bestValue) {
						bestValue = entry.getValue();
						bestAction = entry.getKey();
					}
				}
				
				vTable.put(state, bestValue);
				bestActions.put(state, bestAction);
			}
			
			if (converged(previous_V_table, vTable)) {
				not_converged = false;
			}
		}
	}
	
	public double transitionProbability(TaskDistribution td, State s1, RoadAction action, State s2) {
		double probability = 0.0;
				
		if (action.getActionType() == RoadActionType.MOVE) {
			// check if the action is legal
			City destination = s1.getDestinationCity();
			if (destination == null && action.getNextCity() == s2.getCurrentCity() && s1.getCurrentCity().neighbors().contains(action.getNextCity())) {
				probability = this.highestTaskPotentialNeighbour(s1.getCurrentCity().neighbors(), td);
			}
		} else if (action.getActionType() == RoadActionType.PICKUP) {
			// check if pickup action is legal
			if (s1.getDestinationCity() == null && action.getNextCity() == s2.getCurrentCity()) {
				probability = td.probability(s1.getCurrentCity(), action.getNextCity());
			}
		}
		
		return probability;
	}
	
	public double discountedSum(TaskDistribution td, State state, RoadAction action) {
		double sum = 0.0;
		for (State state_iter : this.possibleStates) {
			double tp = transitionProbability(td, state, action, state_iter);
			sum += tp * vTable.get(state_iter);
			if (vTable.get(state_iter) != 0.0 && tp != 0.0) {
				System.out.println("TP = " + tp + " :: vTable = " + vTable.get(state_iter));
			}
		}
		
		sum = this.pPickup * sum;
		return sum;
	}
	
	//Implementation of the "good enough" part of the algorithm
	public boolean converged(HashMap<State, Double> previous_V_table, HashMap<State, Double> current_V_table) {
		double max = 0.0;
		for (State state : previous_V_table.keySet()) {
			double difference = Math.abs(previous_V_table.get(state) - current_V_table.get(state));
			if (difference > max) {
				max = difference;
			}
		}
		
		return max < 0.001;
	}
	
	// Initialize Tables
	private void initQTable() {
		qTable = new HashMap<State, HashMap<RoadAction, Double>>();
		for (State state : this.possibleStates) {
			qTable.put(state, new HashMap<RoadAction, Double>());
			for (RoadAction action : this.possibleActions) {		
				qTable.get(state).put(action, 0.0);
			}
		}
	}
	
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
			for (City nextCity : city.neighbors()) {
				// Should we only add neighboring cities for the move action?
				// TODO: No ^, we don't have a notion of current city here, so just add all cities
				this.possibleActions.add(new RoadAction(nextCity, RoadActionType.MOVE));
			}
		}
		this.possibleActions.add(new RoadAction(null, RoadActionType.PICKUP));
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
	
	private void initTotalTaskProbabilities(List<City> cities, TaskDistribution td) {
		for (City from : cities) {
			double totalProbability = 0.0;
			for (City to : cities) {
				if (from != to) {
					totalProbability += td.probability(from, to);
				}
			}
			this.totalTaskProbabilities.put(from, totalProbability);
		}
	}
	
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
}

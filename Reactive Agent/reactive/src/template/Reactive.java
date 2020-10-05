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
	
	// Q-table
	private Map<State, HashMap<RoadAction, Double>> qTable;
	// Value table
	private HashMap<State, Double> vTable;
	// Best action
    private HashMap<State, RoadAction> bestActions;
	// Reward table
	private Map<State, HashMap<RoadAction, Double>> rTable;
	// A mapping of probabilities that there is a task from a given city
	private Map<City, Double> totalTaskProbabilities;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);
		
		// ??? Can we do that instead of having to pass vehicle in the initRTable() function???
		//Integer costPerKm = agent.readProperty("cost-per-km", Integer.class, 5);
		
		this.random = new Random();
		this.pPickup = discount;
		//this.costPerKm = costPerKm;
		this.numActions = 0;
		this.myAgent = agent;
		
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
		//TO DO 
		
		Action action;
		
		State currentState = new State(vehicle.getCurrentCity());
		
		if(availableTask != null) {
			currentState.setDestinationCity(availableTask.deliveryCity);
		}
		
		RoadAction bestAction = bestActions.get(currentState);
		
		
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
		while (true) {
			HashMap<State, Double> previous_V_table = new HashMap<State, Double> (vTable);

			for (State state : qTable.keySet()) {
				for (RoadAction action : qTable.get(state).keySet()) {			
					double value = rTable.get(state).get(action) + discountedSum(td, state, action);
					qTable.get(state).put(action, value);
				}
				//Find best action (java 8 implementation)
				RoadAction bestAction = qTable.get(state).entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
				Double bestValue = qTable.get(state).get(bestAction);

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
			if (s1.getCurrentCity().neighbors().contains(action.getNextCity()) && action.getNextCity() == s2.getCurrentCity()) {
				if (s2.getDestinationCity() == null) {
					// We will not be able to pickup when we get to s2
				} else {
					// We will be able to pickup when we get to s2
				}
			}
		} else if (action.getActionType() == RoadActionType.PICKUP) {
			// check if pickup action is legal
			if (s1.getDestinationCity() != null && s1.getDestinationCity() == s2.getCurrentCity()) {
				if (s2.getDestinationCity() == null) {
					// We will not be able to pickup when we get to s2 
				} else {
					// We will be able to pickup when we get to s2 (after we drop off this task at s2's currentCity)
				}
			}
		}
		
		return probability;
	}
	
	public double discountedSum(TaskDistribution td, State state, RoadAction action) {
		double sum = 0.0;
		
		for (State state_iter : this.possibleStates) {
			sum += transitionProbability(td, state, action, state_iter) * vTable.get(state_iter);
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
		for (State state : this.possibleStates) {
			qTable.put(state, new HashMap<RoadAction, Double>());
			for (RoadAction action : this.possibleActions) {		
				qTable.get(state).put(action, 0.0);
			}
		}
	}
	
	private void initVTable() {
		for (State state : this.possibleStates) {
			vTable.put(state, 0.0);
		}
	}
	
	private void initBestActionsTable() {
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
				if (action.getActionType() == RoadActionType.MOVE) {
					if (actionNextCity != null && currentCity.hasNeighbor(actionNextCity)) {
						// Reward is negative - only cost for moving to next city
						reward -= currentCity.distanceTo(actionNextCity) * v.costPerKm();
						stateRewards.put(action, reward);
					}
				} else if (action.getActionType() == RoadActionType.PICKUP) {
					if (state.getDestinationCity() != null) {
						// Only allowed to pickup if we have a destination city?
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
}

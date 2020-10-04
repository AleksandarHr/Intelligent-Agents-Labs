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
	
	// Q-table
	private Map<State, HashMap<RoadAction, Double>> qTable;
	// Value table
	private HashMap<State, Double> vTable;
	// Best action
    private HashMap<State, RoadAction> bestActions;
	// Reward table
	private Map<State, HashMap<RoadAction, Double>> rTable;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class, 0.95);

		this.random = new Random();
		this.pPickup = discount;
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
		action = new Pickup(availableTask);

//		if (availableTask == null || random.nextDouble() > pPickup) {
//			City currentCity = vehicle.getCurrentCity();
//			action = new Move(currentCity.randomNeighbor(random));
//			System.out.println("Leave the task and MOVE to neighbor city");
//		} else {
//			action = new Pickup(availableTask);
//			System.out.println("PICKUP the task");
//		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
	
	public void reinforcementLearningAlgorithm(TaskDistribution td) {
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
			
			if (converged(previous_V_table, vTable)) break;
		}
	}
	
	public double transitionProbability(TaskDistribution td, State s1, RoadAction a, State s2) {
		//TO DO
		double probability = 0.0;
		if(s2.getDestinationCity() == s2.getCurrentCity()) {
			probability = 0.0;
		} else {
			
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
				//TO DO implement to skips some states			
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
			if (state.getDestinationCity() == null) {
				// We do not have a task right now
				rTable.put(state, new HashMap<RoadAction, Double>());
				for (RoadAction action : this.possibleActions) {
					City currentCity = state.getCurrentCity();
					if (action.getActionType() == RoadActionType.MOVE) {
						if (currentCity.neighbors().contains(action.getNextCity())) {
							double reward = (-1.0)* currentCity.distanceTo(action.getNextCity()) * v.costPerKm();
							 stateRewards.put(action, reward);
						} else {
						// not a neighboring city, no reward data
						}
					} else if (action.getActionType() == RoadActionType.PICKUP) {
						double reward = td.reward(state.getCurrentCity(), state.getDestinationCity()) - state.getCurrentCity().distanceTo(state.getDestinationCity());
						stateRewards.put(action, reward);
					}
				}
			}
		}
	}
	
	// HELPER FUNCTIONS
	private void initPossibleActions(List<City> cities) {
		for (City city : cities) {
			for (City nextCity : city.neighbors()) {
				// Should we only add neighboring cities for the move action?
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
}

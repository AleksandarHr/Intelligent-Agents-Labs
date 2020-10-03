package template;

import java.util.Random;

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

import java.util.HashMap;
import java.util.Map;

public class Reactive implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	
	
	// Q-table
	private Map<State, HashMap<RoadAction, Double>> Q_table;
	// Value table
	private HashMap<State, Double> V_table;
	// Best action
    private HashMap<State, RoadAction> Best_actions;
	// Reward table
	private Map<State, HashMap<RoadAction, Double>> R_table;

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
		RoadAction.initActions(topology.cities());
		State.initStates(topology.cities());
		
		//Populate the tables
		initQ_table();
		initV_table();
		initBestActions_table();
		initR_table();

		reinforcement_learning_algorithm();
	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		//TO DO
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
			System.out.println("Leave the task and MOVE to neighbor city");
		} else {
			action = new Pickup(availableTask);
			System.out.println("PICKUP the task");
		}
		
		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}
	
	public void reinforcement_learning_algorithm() {
		while (true) {
			HashMap<State, Double> previous_V_table = new HashMap<State, Double> (V_table);

			for (State state : Q_table.keySet()) {
				for (RoadAction action : Q_table.get(state).keySet()) {			
					double value = R_table.get(state).get(action) + discountedSum(state, action);
					Q_table.get(state).put(action, value);
				}
				//Find best action (java 8 implementation)
				RoadAction bestAction = Q_table.get(state).entrySet().stream().max((entry1, entry2) -> entry1.getValue() > entry2.getValue() ? 1 : -1).get().getKey();
				Double bestValue = Q_table.get(state).get(bestAction);

				V_table.put(state, bestValue);
				Best_actions.put(state, bestAction);
			}
			
			if (converged(previous_V_table, V_table)) break;
		}
	}
	
	public double transition_probability(State s1, RoadAction a, State s2) {
		//TO DO
		return 0.0;
	}
	
	public double discountedSum(State state, RoadAction action) {
		double sum = 0.0;
		
		for (State state_iter : State.getStates()) {
			sum += transition_probability(state, action, state_iter) * V_table.get(state_iter);
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
	
	//Initialize Q table to 0
	public void initQ_table() {
		for (State state : State.getStates()) {
			Q_table.put(state, new HashMap<RoadAction, Double>());
			for (RoadAction action : RoadAction.getActions()) {
				//TO DO implement to skips some states
				
				Q_table.get(state).put(action, 0.0);
			}
		}
	}
	
	public void initV_table() {
		for (State state : State.getStates()) {
			V_table.put(state, 0.0);
		}
	}
	
	public void initBestActions_table() {
		for (State state : State.getStates()) {
			Best_actions.put(state, null);
		}
	}
	
	public void initR_table() {
		//Initialize R table
	}
	
	
}

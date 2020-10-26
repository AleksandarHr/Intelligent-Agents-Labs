package template;

/* import table */
import logist.simulation.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Action;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.plan.Action.Delivery;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class Deliberative implements DeliberativeBehavior {

	enum Algorithm {
		BFS, ASTAR
	}

	/* Environment */
	Topology topology;
	TaskDistribution td;

	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;

		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "ASTAR");

		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());

		// ...
	}

	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;
		State initialState = new State(vehicle, tasks);
		for (Task t : tasks) {
			System.out.println(t);
		}
		// Agent's plan got interrupted and is recomputing a new plan
		if (vehicle.getCurrentTasks() != null && vehicle.getCurrentTasks().size() != 0) {
			// Add the tasks the agent has currently picked up to the new plan
			initialState.setRunningTasks(vehicle.getCurrentTasks());
		}

		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			System.out.println("Planning start.\n");
			long startTime_star = System.nanoTime();
			plan = ASTAR(initialState);
			System.out.println("Cost of ASTAR is: " + plan.totalDistance());
			long endTime_star = System.nanoTime();
			long duration_star = TimeUnit.MILLISECONDS.convert((endTime_star - startTime_star), TimeUnit.NANOSECONDS);
			System.out.println("Planning end after " + duration_star + " miliseconds.\n");
			break;
		case BFS:
			// TODO: Time how long the search takes
			System.out.println("Planning start.\n");
			long startTime = System.nanoTime();
			plan = BFS(initialState);
			System.out.println("Cost of BFS is: " + plan.totalDistance());
			long endTime = System.nanoTime();
			long duration = TimeUnit.MILLISECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
			System.out.println("Planning end after " + duration + " miliseconds.\n");
			break;
		default:
			throw new AssertionError("Should not happen.");
		}
		return plan;
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {

		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}

	/*
	 * BFS performs a Breadth-First-Search starting from a given initial state and
	 * returns the optimal final state, pruning some sub-optimal paths along the way
	 */
	private Plan BFS(State initial) {
		List<State> finalStates = new ArrayList<State>();
		State bestFinalState = null;
		Queue<State> queue = new LinkedList<State>();
		queue.add(initial);
		int counter = 0;
		Set<State> hashSetStates = new HashSet<State>();
		HashMap<State, Double> costStates = new HashMap<State, Double>();
		HashMap<State, State> parentStates = new HashMap<State, State>();
		costStates.put(initial, 0.0);
		parentStates.put(initial, null);

		while (!queue.isEmpty()) {
			State next = queue.poll();
			counter++;
			// Check if we have already reached n with lesser cost
			// Continue exploring next state only if we have not reached any final state yet
			// or if the best final state so far is more expensive than the next state

			if (next.isStateFinal()) {
				// If next is a final state, it must be more optimal than the best-so-far
				if(bestFinalState == null || costStates.get(next) < costStates.get(bestFinalState)) {
					bestFinalState = next;
				}
			} else {
				// If next is not a final state, generate its successor states and add them to
				// the queue
				List<State> successors = next.generateSuccessorStates();
				for (State s : successors) {
					if (!hashSetStates.contains(s)) {
						queue.add(s);
						hashSetStates.add(s);
						costStates.put(s,
								costStates.get(next) + next.getCurrentLocation().distanceTo(s.getCurrentLocation())
										* next.getVehicle().costPerKm());
						parentStates.put(s, next);
					}
					//Update the cost of a state
					if (costStates.get(s) > costStates.get(next)
							+ next.getCurrentLocation().distanceTo(s.getCurrentLocation())
									* next.getVehicle().costPerKm()) {
						costStates.put(s,costStates.get(next)
								+ next.getCurrentLocation().distanceTo(s.getCurrentLocation())
								* next.getVehicle().costPerKm());
						parentStates.put(s, next);
					}
				}
			}

		}
		System.out.println(counter);

		return createPath(parentStates, bestFinalState, initial.getCurrentLocation());
	}
	
	/*
	 * Builds a plan from the sequence of actions from the final state Populates the
	 * missing actions between pickups and deliveries with appropriate move actions
	 */

	public Plan createPath(HashMap<State, State> states, State finalState, City initialCity) {
		List<Action> cityPath = new ArrayList<Action>();
		State currentState = finalState;
		while(states.get(currentState) != null) {
			State parent = states.get(currentState);
			if(currentState.getRunningTasks().size() > parent.getRunningTasks().size()) {
				//Checks for the difference between task to deliver and picked up tasks
				TaskSet iter = TaskSet.intersectComplement(currentState.getRunningTasks(), parent.getRunningTasks());
				Task firstTask = null;
				for(Task t : iter) {
					firstTask = t;
				}
				cityPath.add(new Pickup(firstTask));
			}
			if(currentState.getRunningTasks().size() < parent.getRunningTasks().size()) {
				TaskSet iter = TaskSet.intersectComplement(parent.getRunningTasks(), currentState.getRunningTasks());
				Task firstTask = null;
				for(Task t : iter) {
					firstTask = t;
				}
				cityPath.add(new Delivery(firstTask));
			}
			if(parent.getCurrentLocation() != currentState.getCurrentLocation()) {
				List<Action> cityMoves = new ArrayList<Action>();
				for (City city : parent.getCurrentLocation().pathTo(currentState.getCurrentLocation())) {
					cityMoves.add(new Move(city));
				}
				Collections.reverse(cityMoves);
				cityPath.addAll(cityMoves);
			}
			currentState = parent;
		}
		Collections.reverse(cityPath);
		return new Plan(initialCity, cityPath);
	}

	/*
	 * ASTAR explores the nodes with the lowest cost (cost plus heuristics) first by
	 * sorting it in a priority queue
	 */
	
	public Plan ASTAR(State initial) {
		int counter = 0;
		State bestFinalState = null;
		PriorityQueue<StateComparator> queue = new PriorityQueue<StateComparator>();
		Set<State> hashSetStates = new HashSet<State>();
		HashMap<State, Double> costStates = new HashMap<State, Double>();
		HashMap<State, State> parentStates = new HashMap<State, State>();
		costStates.put(initial, 0.0);
		parentStates.put(initial, null);
		queue.add(new StateComparator(initial, initial.computeHeuristics()));
		System.out.println(initial.getHeuristics());

		while (!queue.isEmpty()) {
			counter++;
			StateComparator sc = queue.poll();
			State next = sc.getState();
			Double cost = sc.getCost();
			// Check if we have already reached n with lesser cost
			// Continue exploring next state only if we have not reached any final state yet
			// or if the best final state so far is more expensive than the next state

			if (next.isStateFinal()) {
				// If next is a final state, it must be more optimal than the best-so-far
				System.out.println(counter);
				return createPath(parentStates, next, initial.getCurrentLocation());
				// hashSetStates.add(next);
			} else {
				// If next is not a final state, generate its successor states and add them to
				// the queue
				List<State> successors = next.generateSuccessorStates();
				for (State s : successors) {
					
					if (!hashSetStates.contains(s) || costStates.get(s) > costStates.get(next)
							+ next.getCurrentLocation().distanceTo(s.getCurrentLocation())
									* next.getVehicle().costPerKm()) {
						costStates.put(s,costStates.get(next)
								+ next.getCurrentLocation().distanceTo(s.getCurrentLocation())
								* next.getVehicle().costPerKm());
						
						parentStates.put(s, next);
						queue.add(new StateComparator(s, costStates.get(next)
								+ next.getCurrentLocation().distanceTo(s.getCurrentLocation())
								* next.getVehicle().costPerKm() + s.computeHeuristics()));
						hashSetStates.add(s);
					}
				}
			}


		}

		return createPath(parentStates, bestFinalState, initial.getCurrentLocation());
	}



}

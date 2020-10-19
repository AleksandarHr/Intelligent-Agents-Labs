package template;

/* import table */
import logist.simulation.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

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
		boolean heuristics = false;
		State initialState = new State(vehicle, tasks, heuristics);
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
			State finalState_star = ASTAR(initialState);
			long endTime_star = System.nanoTime();
			long duration_star = TimeUnit.SECONDS.convert((endTime_star - startTime_star), TimeUnit.NANOSECONDS);
			System.out.println("Planning end after " + duration_star + " seconds.\n");

			plan = getPlan(finalState_star, vehicle);
			break;
		case BFS:
			// TODO: Time how long the search takes
			System.out.println("Planning start.\n");
			long startTime = System.nanoTime();
			State finalState = BFS(initialState);
			long endTime = System.nanoTime();
			long duration = TimeUnit.SECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
			System.out.println("Planning end after " + duration + " seconds.\n");

			plan = getPlan(finalState, vehicle);
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
	private State BFS(State initial) {
		List<State> finalStates = new ArrayList<State>();
		State bestFinalState = null;
		// BFS Search
		Queue<State> queue = new LinkedList<State>();
		queue.add(initial);
		int counter = 0;
		Set<State> hashSetStates = new HashSet<State>();

		while (!queue.isEmpty()) {
			State next = queue.poll();
			counter++;
			// Check if we have already reached n with lesser cost
			// Continue exploring next state only if we have not reached any final state yet
			// or if the best final state so far is more expensive than the next state
			if (bestFinalState == null || bestFinalState.getCost() > next.getCost()) {
				if (next.isStateFinal()) {
					// If next is a final state, it must be more optimal than the best-so-far
					bestFinalState = next;
					hashSetStates.add(next);
				} else {
					// If next is not a final state, generate its successor states and add them to
					// the queue
					List<State> successors = next.generateSuccessorStates();
					for (State s : successors) {
						if (!hashSetStates.contains(s)) {
							queue.add(s);
							hashSetStates.add(s);
						}
					}
				}
			}

		}
		System.out.println(counter);

		return bestFinalState;
	}

	/*
	 * ASTAR explores the nodes with the lowest cost (cost plus heuristics) first by
	 * sorting it in a priority queue
	 */

	public State ASTAR(State initial) {
		StateComparator compare = new StateComparator();
		PriorityQueue<State> queue = new PriorityQueue<State>(100000, compare);
		Set<State> hashSetStates = new HashSet<State>();
		State next = null;
		queue.add(initial);

		int counter = 0;

		while (!queue.isEmpty()) {
			// Returns and remove the element at the top of the Queue
			next = queue.poll();
			if (next.isStateFinal()) {
				return next;
			}


			List<State> successors = next.generateSuccessorStates();
			for (State s : successors) {
				if (!hashSetStates.contains(s)) {
					queue.add(s);
					hashSetStates.add(s);
				}
			}


		}

		return next;
	}
	/*
	 * Builds a plan from the sequence of actions from the final state Populates the
	 * missing actions between pickups and deliveries with appropriate move actions
	 */

	public Plan getPlan(State finalState, Vehicle vehicle) {
		City startCity = vehicle.getCurrentCity();
		City currentCity = startCity;
		Plan plan = new Plan(startCity);

		for (Tuple action : finalState.pastActions) {
			City destinationCity;
			if (action.type == Tuple.Type.DELIVER) {
				destinationCity = action.task.deliveryCity;
			} else {
				destinationCity = action.task.pickupCity;
			}

			for (City city : currentCity.pathTo(destinationCity)) {
				plan.appendMove(city);
			}

			if (action.type == Tuple.Type.DELIVER) {
				plan.appendDelivery(action.task);
			} else {
				plan.appendPickup(action.task);
			}
			currentCity = destinationCity;

		}

		return plan;
	}

	/*
	 * Simple BFS - returns the first solution it finds, NOT the optimal Used for
	 * testing
	 */
	private State simpleBfs(State initial) {
		// BFS Search
		State firstSolution = null;

		Queue<State> queue = new LinkedList<State>();
		List<State> visited = new ArrayList<State>();
		queue.add(initial);

		while (!queue.isEmpty()) {
			System.out.println("Queue length = " + queue.size());
			State next = queue.poll();
			if (next.isStateFinal()) {
				firstSolution = next;
				break;
			}

			// Check if we have already reached n with lesser cost
			if (!next.isStateRedundant(visited)) {
				// n.printState();
				visited.add(next);
				List<State> successors = next.generateSuccessorStates();
				queue.addAll(successors);
			}
		}

		return firstSolution;
	}
}

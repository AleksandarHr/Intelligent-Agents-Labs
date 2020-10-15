package template;

/* import table */
import logist.simulation.Vehicle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
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
		// TODO: we need to create a new state object here, so we need a constructor which takes Vehicle and TaskSet?
		State initialState = new State(vehicle, tasks);
		
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			// ...
			plan = naivePlan(vehicle, tasks);
			break;
		case BFS:
			// TODO: Time how long the search takes
			System.out.println("Planning start.\n");
			long startTime = System.nanoTime();
			//State finalState = simpleBfs(initialState);
			State finalState = BFS(initialState);
			long endTime = System.nanoTime();
			long duration = TimeUnit.SECONDS.convert((endTime - startTime), TimeUnit.NANOSECONDS);
			System.out.println("Planning end after " + duration + " seconds.\n");
			
			plan = finalState.getPlan();
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


	// Simple BFS - returns the first solution it finds, NOT the optimal - can use for testing?
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
	
	
	// BFS - returns optimal solution found
	private State BFS(State initial) {
		List<State> finalStates = new ArrayList<State>();
		State bestFinalState = null;
		// BFS Search
		Queue<State> queue = new LinkedList<State>();
		List<State> visited = new ArrayList<State>();
		queue.add(initial);

		while (!queue.isEmpty()) {
			System.out.println("QUEUE LENGTH = " + queue.size());
			State next = queue.poll();

			// Check if we have already reached n with lesser cost
			if (!next.isStateRedundant(visited)) {
				// n.printState();
				visited.add(next);
				if (bestFinalState == null || bestFinalState.getCost() > next.getCost()) {
					if (next.isStateFinal()) {
						bestFinalState = next;
					} 
					else {
						List<State> successors = next.generateSuccessorStates();
						queue.addAll(successors);
					}
				}
			}
		}

//		System.out.println("BFS found " + finalStates.size() + " leaf states.\n");
//		State optimalState = finalStates.get(0);
//		double optimalCost = finalStates.get(0).getCost();
//
//		for (State s : finalStates) {
//			if (s.getCost() < optimalCost) {
//				optimalCost = s.getCost();
//				optimalState = s;
//			}
//		}

		return bestFinalState;
	}

	public State ASTAR(State initial) {
		StateComparator compare = new StateComparator();
		PriorityQueue<State> Q = new PriorityQueue<State>(100000, compare);
		//List<State> Q = new LinkedList<State>();
		List<State> visited = new ArrayList<State>();
		State n = null;
        
		Q.add(initial);
        
        while (!Q.isEmpty()) {
        	n = Q.poll();
        	
        	if (n.isStateFinal()) {
        		return n;
        	}
        	
        	
        	if (n.isStateRedundantOrLowerCost(visited)) {
        		// add n to C
        		visited.add(n);
        		// add successors of n
        		n.generateSuccessorStates();
        		Q.addAll(n.getSuccessorStates());
        	}

        }
        
		return n;
	}

}

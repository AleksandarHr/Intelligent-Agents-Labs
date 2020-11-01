package template;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import logist.LogistSettings;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.behavior.CentralizedBehavior;
import logist.agent.Agent;
import logist.config.Parsers;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import template.CentralizedAction.actionType;

/**
 * A very simple auction agent that assigns all tasks to its first vehicle and
 * handles them sequentially.
 *
 */
@SuppressWarnings("unused")
public class CentralizedTemplate implements CentralizedBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private long timeout_setup;
	private long timeout_plan;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

		// this code is used to get the timeouts
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
		} catch (Exception exc) {
			System.out.println("There was a problem loading the configuration file.");
		}

		// the setup method cannot last more than timeout_setup milliseconds
		timeout_setup = ls.get(LogistSettings.TimeoutKey.SETUP);
		// the plan method cannot execute more than timeout_plan milliseconds
		timeout_plan = ls.get(LogistSettings.TimeoutKey.PLAN);

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
		long time_start = System.currentTimeMillis();
		int iterationsBound = 10000;
		double p = 0.3;
		List<Plan> plans = slsPlans(vehicles, tasks, iterationsBound, p, time_start);
		double cost = 0.0;
		for (int i = 0; i < plans.size(); i++) {
			cost += plans.get(i).totalDistance() * vehicles.get(i).costPerKm();
		}
		System.out.println("TOTAL COST = " + cost);
		long time_end = System.currentTimeMillis();
		long duration = time_end - time_start;
		System.out.println("Plan generated in " + duration);
		return plans;
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);
		double cost = 0.0;
		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity)) {
				plan.appendMove(city);
			}
			cost += current.distanceTo(task.pickupCity);
			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path()) {
				plan.appendMove(city);
			}

			plan.appendDelivery(task);
			cost += task.pickupCity.distanceTo(task.deliveryCity);
			// set current city
			current = task.deliveryCity;
		}
		System.out.println(cost * vehicle.costPerKm());
		return plan;
	}

	private List<Plan> slsPlans(List<Vehicle> vehicles, TaskSet tasks, int iterationsBound, double p, long startTime) {
		Solution currentBestSolution = new Solution(vehicles, tasks);
		currentBestSolution.createRandomInitialSolution();

		Solution bestSoFar = currentBestSolution;

		int iterationCount = 0;

		while (iterationCount < iterationsBound && timeOut(startTime)) {
			iterationCount++;
			List<Solution> neighbors = currentBestSolution.generateNeighbourSolutions();
			Solution oldBestSolution = currentBestSolution;
			if (Math.random() <= p) {
				Solution bestPlan = currentBestSolution != null ? currentBestSolution : neighbors.get(0);
				double bestCost = bestPlan.computeCost();
				// Find the solution with the best plan
				for (Solution plan : neighbors) {
					double cost = plan.computeCost();
					if (cost <= bestCost) {
						bestPlan = plan;
						bestCost = cost;
					}
				}
				currentBestSolution = bestPlan;
			} else {
				// This should not happen
				if (neighbors.size() > 0) {
					Random random = new Random();
					int index = random.nextInt(neighbors.size());
					currentBestSolution = neighbors.get(index);
				}
			}

			bestSoFar = currentBestSolution.computeCost() < bestSoFar.computeCost() ? currentBestSolution : bestSoFar;

		}

		List<Plan> optimalVehiclePlans = new ArrayList<Plan>(vehicles.size());
		for (Vehicle v : vehicles) {
			LinkedList<CentralizedAction> actions = bestSoFar.getActions().get(v);
			Plan plan = bestSoFar.buildPlanFromActionList(actions, v.getCurrentCity());
			optimalVehiclePlans.add(plan);
		}
		return optimalVehiclePlans;
	}

	private boolean timeOut(long startTime) {
		long currentTime = System.currentTimeMillis();
		long duration = currentTime - startTime;
		//Half of a second to build a plan
		return duration < timeout_plan - 500;
	}

}

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
import template.CentralizedAction.ActionType;

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
	
    enum AlgorithmType{
		NAIVE,
		SLS,
		SLSRANDOM
	}

    @Override
    public void setup(Topology topology, TaskDistribution distribution,
            Agent agent) {
        
        // this code is used to get the timeouts
        LogistSettings ls = null;
        try {
            ls = Parsers.parseSettings("config" + File.separator + "settings_default.xml");
        }
        catch (Exception exc) {
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
        AlgorithmType alg = AlgorithmType.SLSRANDOM;
        List<Plan> plans = new ArrayList<Plan>();
        boolean pickRandom = false;

        if (alg == AlgorithmType.NAIVE) {
        	Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);
        	plans.add(planVehicle1);
        	while (plans.size() < vehicles.size()) {
            	plans.add(Plan.EMPTY);
        	}
        	return plans;
        } else if (alg == AlgorithmType.SLSRANDOM) {
        	pickRandom = true;
        }
        
        int iterationsBound = 10000;
        double p = 0.3;
        plans = slsPlans(vehicles, tasks, iterationsBound, p, pickRandom);
        double cost = 0.0;
        for (int i = 0; i < plans.size(); i++) {
        	cost += plans.get(i).totalDistance() * vehicles.get(i).costPerKm();
        }
        System.out.println("TOTAL COST = " + cost);
        long time_end = System.currentTimeMillis();
        long duration = time_end - time_start;
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
    
    // Stochastic Local Search
    private List<Plan> slsPlans(List<Vehicle> vehicles, TaskSet tasks, int iterationsBound, double p, boolean pickRandom){
    	Solution currentBestSolution = new Solution(vehicles, tasks);
    	currentBestSolution.createRandomInitialSolution();

		Solution bestSoFar = currentBestSolution;

		int iterationCount = 0;
		
		// iterate until we reach iterationsBound
		while (iterationCount < iterationsBound) {
			iterationCount++;
			// generate neigbhours of current best solution
			List<Solution> neighbors = currentBestSolution.generateNeighbourSolutions();
			Solution previousBestSolution = currentBestSolution;
			if (Math.random() <= p) {
				// with probability p, proceed with the best neighbor solution
				Solution bestPlan = currentBestSolution != null ? currentBestSolution : neighbors.get(0);
				double bestCost = bestPlan.computeCost();
				//Find the solution with the best plan
				for (Solution plan : neighbors) {
					double cost = plan.computeCost();
					if (cost <= bestCost) {
						bestPlan = plan;
						bestCost = cost;
					}
				}
				currentBestSolution = bestPlan;
			} else {
				// with probability (1-p) either revert back to the previous best solution
				//		or pick a random neighbour (depending on boolean argument)
				if(neighbors.size() > 0) {
					if (pickRandom) {
						Random random = new Random();
						int index = random.nextInt(neighbors.size());
						currentBestSolution = neighbors.get(index);
					} else {
						currentBestSolution = previousBestSolution;
					}
				} 
			}
					
			bestSoFar = currentBestSolution.computeCost() < bestSoFar.computeCost() ? currentBestSolution : bestSoFar;
		}
		
		// Build logist plan for every vehicle from the actions in the best solution found
		List<Plan> optimalVehiclePlans = new ArrayList<Plan>(vehicles.size());
		for (Vehicle v : vehicles) {
    		LinkedList<CentralizedAction> actions = bestSoFar.getActions().get(v);
    		Plan plan = bestSoFar.buildPlanFromActionList(actions, v.getCurrentCity());
    		optimalVehiclePlans.add(plan);
    	}

		System.out.println("TOTAL FINAL COST = " + bestSoFar.computeCost());
		return optimalVehiclePlans;
	}
}

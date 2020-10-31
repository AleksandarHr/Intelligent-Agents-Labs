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
        
//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);
//        Plan planVehicle1 = naivePlan(vehicles.get(0), tasks);
//
//        List<Plan> plans = new ArrayList<Plan>();
//        plans.add(planVehicle1);
//        while (plans.size() < vehicles.size()) {
//            plans.add(Plan.EMPTY);
//        }
        
        int iterationsBound = 10000;
        double p = 0.3;
        List<Plan> plans = slsPlans(vehicles, tasks, iterationsBound, p);
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
    
    private List<Plan> slsPlans(List<Vehicle> vehicles, TaskSet tasks, int iterationsBound, double p) {
    	List<Plan> optimalVehiclePlans = new ArrayList<Plan>(vehicles.size());
    	
    	Solution currentBestSolution = new Solution(vehicles, tasks);
    	currentBestSolution = currentBestSolution.createRandomInitialSolution();
    	System.out.println("INITIAL SOLUTIONS");
//    	currentBestSolution = currentBestSolution.createInitialSolution();
    	double currentMinimalCost = Double.MAX_VALUE;
    	int counter = 0;
    	while(counter < iterationsBound) {
        	Solution oldSolution = currentBestSolution.solutionDeepCopy();
    		List<Solution> neighbourSolutions = currentBestSolution.generateNeighbourSolutions();
    		for (Solution neighbour : neighbourSolutions) {
    			HashMap<Vehicle, LinkedList<CentralizedAction>> solutionActions = neighbour.getActions();
    			HashMap<Vehicle, Double> allCosts = computeCostsForAllVehicles(vehicles, solutionActions);
    			double tempCost = 0.0;
    			for (Vehicle v : vehicles) {
    				tempCost += allCosts.get(v);
    			}
    			if (tempCost < currentMinimalCost) {
    				currentBestSolution = neighbour;
    				currentMinimalCost = tempCost;
    			}
    		}
        	
    		if (Math.random() <= p) {
    			// with probability 1-p, we keep the old solution
//    			currentBestSolution = oldSolution;
    			Random rand = new Random();
    			if (neighbourSolutions.size() > 0) {
    				currentBestSolution = neighbourSolutions.get(rand.nextInt(neighbourSolutions.size()));
    			} else {
    				currentBestSolution = oldSolution;
    			}
    		}
    		counter ++;
    	}

    	for (Vehicle v : vehicles) {
    		LinkedList<CentralizedAction> actions = currentBestSolution.getActions().get(v);
    		Plan plan = currentBestSolution.buildPlanFromActionList(actions, v.getCurrentCity());
    		optimalVehiclePlans.add(plan);
    	}
//    	System.out.println("TOTAL FINAL COST = " + currentMinimalCost);
    	return optimalVehiclePlans;
    }
    
	// Compute all vehicles' plans' costs and return as hashmap
    private HashMap<Vehicle, Double> computeCostsForAllVehicles(List<Vehicle> vehicles, HashMap<Vehicle, LinkedList<CentralizedAction>> allActions) {
    	HashMap<Vehicle, Double> allCosts = new HashMap<Vehicle, Double>();
    	
    	for (Vehicle v : vehicles) {
    		List<CentralizedAction> vehicleActions = allActions.get(v);
    		double vehicleCost = computeCostForVehicle(v, vehicleActions);
    		allCosts.put(v, vehicleCost);
    	}
    	
    	return allCosts;
    }
    
    // Given a vehicle and it's planned actions, compute total cost
    private double computeCostForVehicle(Vehicle v, List<CentralizedAction> actions) {
    	double cost = 0.0;
    	City currentLocation = v.getCurrentCity();
    	
    	for (CentralizedAction a : actions) {
    		City pickupCity = a.getCurrentTask().pickupCity;
    		City deliveryCity = a.getCurrentTask().deliveryCity;
    		if (a.getType() == actionType.PICKUP) {
    			cost += currentLocation.distanceTo(pickupCity) * v.costPerKm();
    			currentLocation = pickupCity;
    		} else if (a.getType() == actionType.DELIVER) {
    			cost += currentLocation.distanceTo(deliveryCity) * v.costPerKm();
    			currentLocation = deliveryCity;
    		}
    	}
    	
    	return cost;
    }
}

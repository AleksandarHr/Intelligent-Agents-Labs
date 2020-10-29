package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;
import template.CentralizedAction.actionType;

public class Solution {
	
	private List<Vehicle> vehicles;
	private HashMap<Vehicle, Plan> plans;
	private HashMap<Vehicle, LinkedList<CentralizedAction>> actions;
	private TaskSet tasks;
	
	public Solution(List<Vehicle> vehicles, TaskSet tasks) {
		this.plans = new HashMap<Vehicle, Plan>();	
		this.actions = new HashMap<Vehicle, LinkedList<CentralizedAction>>();
		this.vehicles = vehicles;
		this.tasks = tasks.clone();
	}
	
	// Initial solution assigns all tasks to the biggest vehicle available
	public Solution createInitialSolution() {
		Vehicle biggestVehicle = null;
		for (Vehicle v : this.vehicles) {
			if (biggestVehicle == null || biggestVehicle.capacity() < v.capacity()) {
				biggestVehicle = v;
			}
		}
		
		// If there is a task which exceeds the capacity of the biggest vehicle
		// then there is no solution
		for (Task t : this.tasks) {
			if (t.weight > biggestVehicle.capacity()) {
				System.out.print("Biggest vehicle is not big enough.");
				return null;
			}
		}
		
		Solution initialSolution = buildInitialPlan(biggestVehicle);
		return initialSolution;
	}
	
	private Solution buildInitialPlan(Vehicle biggestVehicle) {
		Solution initialSolution = new Solution(this.vehicles, this.tasks);
		
		LinkedList<CentralizedAction> vehicleActions = new LinkedList<CentralizedAction>();
		for (Task t : initialSolution.tasks) {
			CentralizedAction pickupAction = new CentralizedAction(t, actionType.PICKUP);
			CentralizedAction deliverAction = new CentralizedAction(t, actionType.DELIVER);
			vehicleActions.add(pickupAction);
			vehicleActions.add(deliverAction);
		}
		
		Plan plan = buildPlanFromActionList(vehicleActions, biggestVehicle.getCurrentCity());
		
		for (Vehicle v : initialSolution.vehicles) {
			if (v.equals(biggestVehicle)) {
				initialSolution.plans.put(v, plan);
				initialSolution.actions.put(v, vehicleActions);
			} else {
				initialSolution.plans.put(v, Plan.EMPTY);
				initialSolution.actions.put(v, new LinkedList<CentralizedAction>());
			}
		}
		
		return initialSolution;
	}
	
	
	// Move firrst task of the 'from' vehicle to any other vehicle possible
	private List<Solution> changeFirstTaskAgent(Vehicle from) {
		List<Solution> neighbourSolutions = new ArrayList<Solution>();
		HashMap<Vehicle, LinkedList<CentralizedAction>> allActions = mapDeepCopy(this.actions);

		// get the first task to be picked up by the 'from' vehicle
		LinkedList<CentralizedAction> vehicleActions = allActions.get(from);
		Task taskToChange = vehicleActions.get(0).getCurrentTask();
		// remove the task pickup from the plan
		vehicleActions.remove(0);
		
		for (CentralizedAction action : vehicleActions) {
			if (action.getCurrentTask().equals(taskToChange)) {
				// remove the task delivery from the plan
				vehicleActions.remove(action);
			}
		}
		
		for (Vehicle to : this.vehicles) {
			if (!to.equals(from)) {
				if (to.capacity() >= taskToChange.weight) {
					// only add the task to a vehicle with sufficient capacity
					LinkedList<CentralizedAction> newActions = new LinkedList<CentralizedAction>(allActions.get(to));
					newActions.addFirst(new CentralizedAction(taskToChange, actionType.DELIVER));
					newActions.addFirst(new CentralizedAction(taskToChange, actionType.PICKUP));
					allActions.put(to, newActions);			

					Solution solution = new Solution(this.vehicles, this.tasks);
					solution.setActions(allActions);
					neighbourSolutions.add(solution);
				}
			}
		}
		
		return neighbourSolutions;
	}

	
	
	
	
	// Given a list of CentralizedAction objects build the corresponding logist plan
	private Plan buildPlanFromActionList(LinkedList<CentralizedAction> actions, City initialCity) {
		City currentLocation = initialCity;
		Plan p = new Plan(initialCity);
		for (CentralizedAction a : actions) {
			Task t = a.getCurrentTask();
			if (a.getType() == actionType.PICKUP) {
				for (City c : currentLocation.pathTo(t.pickupCity)) {
					p.appendMove(c);
				}
				currentLocation = t.pickupCity;
				p.appendPickup(a.getCurrentTask());
			} else if (a.getType() == actionType.DELIVER) {
				for (City c : currentLocation.pathTo(t.deliveryCity)) {
					p.appendMove(c);
				}
				currentLocation = t.deliveryCity;
				p.appendDelivery(t);
			}
		}
			
		return p;
	}
	
	// Creates and returns a deep copy of provided hashmap
	private HashMap<Vehicle, LinkedList<CentralizedAction>> mapDeepCopy(HashMap<Vehicle, LinkedList<CentralizedAction>> map) {
		HashMap<Vehicle, LinkedList<CentralizedAction>> copy = new HashMap<Vehicle, LinkedList<CentralizedAction>>();
		for (Map.Entry<Vehicle, LinkedList<CentralizedAction>> entry : map.entrySet()) {
			copy.put(entry.getKey(), new LinkedList<CentralizedAction>(entry.getValue()));
		}
		
		return copy;
	}
	
	
	// GETTERS & SETTERS
	public HashMap<Vehicle, Plan> getPlans() {
		return plans;
	}

	public void setPlans(HashMap<Vehicle, Plan> plans) {
		this.plans = plans;
	}

	public HashMap<Vehicle, LinkedList<CentralizedAction>> getActions() {
		return actions;
	}

	public void setActions(HashMap<Vehicle, LinkedList<CentralizedAction>> actions) {
		this.actions = actions;
	}
}

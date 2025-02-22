package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;
import template.CentralizedAction.ActionType;

public class Solution {
	
	private List<Vehicle> vehicles;
	private HashMap<Vehicle, Plan> plans;
	private HashMap<Vehicle, LinkedList<CentralizedAction>> actions;
	private TaskSet tasks;
	
	public Solution(List<Vehicle> vehicles, TaskSet tasks) {
		this.plans = new HashMap<Vehicle, Plan>();	
		this.actions = new HashMap<Vehicle, LinkedList<CentralizedAction>>();
		for (Vehicle v : vehicles) {
			this.actions.put(v, new LinkedList<CentralizedAction>());
		}
		this.vehicles = vehicles;
		this.tasks = tasks.clone();
	}
	
	// Initial solution which assigns every task to a large enough random vehicle
	public Solution createRandomInitialSolution() {
		for (Task t : this.tasks) {
			Random rand = new Random();
			Vehicle randomVehicle = this.vehicles.get(rand.nextInt(this.vehicles.size()));
			while (randomVehicle.capacity() < t.weight) {
				randomVehicle = this.vehicles.get(rand.nextInt(this.vehicles.size()));
			}
			LinkedList<CentralizedAction> actionsSoFar = this.actions.get(randomVehicle);
			actionsSoFar.add(new CentralizedAction(t, ActionType.PICKUP));
			actionsSoFar.add(new CentralizedAction(t, ActionType.DELIVER));
			this.actions.put(randomVehicle, actionsSoFar);
		}
		
		Solution initialSolution = new Solution(this.vehicles, this.tasks);
		initialSolution.setActions(this.actions);
		for (Vehicle v : this.vehicles) {
			Plan plan = buildPlanFromActionList(initialSolution.getActions().get(v), v.getCurrentCity());
			this.plans.put(v, plan);
		}
		initialSolution.setPlans(this.plans);
		return initialSolution;
	}

	// Generate neighbour solutions to the current solution - pick a random vehicle
	//		and perform the strategies on it (swap first task, pickup/delivery actions shift)
	public List<Solution> generateNeighbourSolutions() {
		List<Solution> neighbours = new LinkedList<Solution>();
		
		Random rand = new Random();
		Vehicle randVehicle = this.vehicles.get(rand.nextInt(this.vehicles.size()));
		while (this.actions.get(randVehicle).size() == 0) {
			int idx = rand.nextInt(this.vehicles.size());
			randVehicle = this.vehicles.get(idx);			
		}
		neighbours = changeFirstTaskAgent(randVehicle);
		for (int i = 0; i < this.actions.get(randVehicle).size(); i ++ ) {
			if (this.actions.get(randVehicle).get(i).getType() == ActionType.PICKUP) {
				neighbours.addAll(shiftPickupAction(randVehicle, i));
			} else if (this.actions.get(randVehicle).get(i).getType() == ActionType.DELIVER) {
				neighbours.addAll(shiftDeliverAction(randVehicle, i));
			}
		}
		return neighbours;
	}
	
	// Initial solution assigns all tasks to the biggest vehicle available
	public Solution createInitialSolution() {
		Vehicle biggestVehicle = findBiggestVehicle(this.vehicles);
		
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
	
	// Build initial solution by giving all tasks to the largest vehicle
	private Solution buildInitialPlan(Vehicle biggestVehicle) {
		Solution initialSolution = new Solution(this.vehicles, this.tasks);
		
		LinkedList<CentralizedAction> vehicleActions = new LinkedList<CentralizedAction>();
		for (Task t : initialSolution.tasks) {
			CentralizedAction pickupAction = new CentralizedAction(t, ActionType.PICKUP);
			CentralizedAction deliverAction = new CentralizedAction(t, ActionType.DELIVER);
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
		if (vehicleActions.size() <= 0) {
			return neighbourSolutions;
		}
		Task taskToChange = vehicleActions.get(0).getCurrentTask();
		// remove the task pickup from the plan
		vehicleActions.remove(0);
		
		for (int i = 0; i < vehicleActions.size(); i++) {
			if(vehicleActions.get(i).getCurrentTask().equals(taskToChange)) {
				vehicleActions.remove(i);
				break;
			}
		}
	
		for (Vehicle to : this.vehicles) {
			if (!to.equals(from)) {
				// only add the task to a vehicle (if not the 'from' vehicle) with sufficient capacity
				if (to.capacity() >= taskToChange.weight) {
					HashMap<Vehicle, LinkedList<CentralizedAction>> newSolutionActions = mapDeepCopy(this.actions);
					LinkedList<CentralizedAction> newActions = new LinkedList<CentralizedAction>(newSolutionActions.get(to));
					// add pickup & delivery of the new task at the beginning of the vehicle's actions
					newActions.addFirst(new CentralizedAction(taskToChange, ActionType.DELIVER));
					newActions.addFirst(new CentralizedAction(taskToChange, ActionType.PICKUP));
					newSolutionActions.put(to, newActions);			
					newSolutionActions.put(from, vehicleActions);
					
					// create a new solution and add to the list of neighbour solutions
					Solution solution = new Solution(this.vehicles, this.tasks.clone());
					solution.setActions(newSolutionActions);
					neighbourSolutions.add(solution);
				}
			}
		}
		
		return neighbourSolutions;
	}
	
	
	private List<Solution> shiftPickupAction(Vehicle v, int actionSequenceNumber) {
		List<Solution> neighbourSolutions = new LinkedList<Solution>();
		List<CentralizedAction> oldActions = new LinkedList<CentralizedAction>(this.actions.get(v));
		
		CentralizedAction taskToMove = oldActions.get(actionSequenceNumber);
		
		if (actionSequenceNumber < 0 || actionSequenceNumber >= oldActions.size()) {
			return neighbourSolutions;
		}
		
		// shift pickup action to the right
		int moveForwardTo = actionSequenceNumber + 1;	
		while (moveForwardTo < oldActions.size()) {
			if (oldActions.get(moveForwardTo).getCurrentTask().equals(taskToMove.getCurrentTask())) {
				// stop moving pickup action to the right if we reached the delivery action for the same task
				break;
			}
			// update actions list
			LinkedList<CentralizedAction> newSolutionActions = moveActionFromTo(oldActions, actionSequenceNumber, moveForwardTo);
			// make sure capacity constraint is not violated
			if (isCapacityEnough(v, newSolutionActions)) {
				HashMap<Vehicle, LinkedList<CentralizedAction>> allActions = mapDeepCopy(this.actions);
				allActions.put(v, newSolutionActions);
				Solution solution = new Solution(this.vehicles, this.tasks.clone());
				solution.setActions(allActions);
				neighbourSolutions.add(solution);
			}
			moveForwardTo++;
		}
		
		// shift pickup action to the left
		int moveBackwardsTo = actionSequenceNumber - 1;
		while (moveBackwardsTo >= 0) {
			// update actions list
			LinkedList<CentralizedAction> newSolutionActions = moveActionFromTo(oldActions, actionSequenceNumber, moveBackwardsTo);
			// make sure capacity constraint is not violated
			if (isCapacityEnough(v, newSolutionActions)) {
				HashMap<Vehicle, LinkedList<CentralizedAction>> allActions = mapDeepCopy(this.actions);
				allActions.put(v, newSolutionActions);
				Solution solution = new Solution(this.vehicles, this.tasks.clone());
				solution.setActions(allActions);
				neighbourSolutions.add(solution);
			}
			moveBackwardsTo--;
		}
		
		return neighbourSolutions;
	}
	
	private List<Solution> shiftDeliverAction(Vehicle v, int actionSequenceNumber) {
		List<Solution> neighbourSolutions = new LinkedList<Solution>();
		List<CentralizedAction> oldActions = new LinkedList<CentralizedAction>(this.actions.get(v));
		
		CentralizedAction taskToMove = oldActions.get(actionSequenceNumber);
		
		if (actionSequenceNumber < 0 || actionSequenceNumber >= oldActions.size()) {
			return neighbourSolutions;
		}
		
		// shift pickup action to the right
		int moveForwardTo = actionSequenceNumber + 1;	
		while (moveForwardTo < oldActions.size()) {
			// update list of actions
			LinkedList<CentralizedAction> newSolutionActions = moveActionFromTo(oldActions, actionSequenceNumber, moveForwardTo);
			// make sure capacity constraint is not violated
			if (isCapacityEnough(v, newSolutionActions)) {
				HashMap<Vehicle, LinkedList<CentralizedAction>> allActions = mapDeepCopy(this.actions);
				allActions.put(v, newSolutionActions);
				Solution solution = new Solution(this.vehicles, this.tasks.clone());
				solution.setActions(allActions);
				neighbourSolutions.add(solution);
			}
			moveForwardTo++;
		}
		
		// shift pickup action to the left
		int moveBackwardsTo = actionSequenceNumber - 1;
		while (moveBackwardsTo >= 0) {
			if (oldActions.get(moveBackwardsTo).getCurrentTask().equals(taskToMove.getCurrentTask())) {
				// stop moving deliver action to the left if we reached the pickup action for the same task
				break;
			}
			// update the actions list
			LinkedList<CentralizedAction> newSolutionActions = moveActionFromTo(oldActions, actionSequenceNumber, moveBackwardsTo);
			// make sure capacity constraint is not violated
			if (isCapacityEnough(v, newSolutionActions)) {
				HashMap<Vehicle, LinkedList<CentralizedAction>> allActions = mapDeepCopy(this.actions);
				allActions.put(v, newSolutionActions);
				Solution solution = new Solution(this.vehicles, this.tasks.clone());
				solution.setActions(allActions);
				neighbourSolutions.add(solution);
			}
			moveBackwardsTo--;
		}
		
		return neighbourSolutions;

	}
	
	// HELPERS
	
	// Given a list of actions, move action at sequence number "from" to sequence number "to" 
	private LinkedList<CentralizedAction> moveActionFromTo(List<CentralizedAction> actions, int from, int to) {
		LinkedList<CentralizedAction> updatedActions = new LinkedList<CentralizedAction>(actions);
		
		CentralizedAction toMove = updatedActions.remove(from);
		int moveToIdx = from < to ? (to - 1) : to;
		updatedActions.add(moveToIdx, toMove);

		return updatedActions;
	}
	
	// Compute the remaining capacity of a vehicle right before performing action with given sequence number
	private boolean isCapacityEnough(Vehicle v, List<CentralizedAction> vehicleActions) {
		int runningLoad = 0;
		for (CentralizedAction a : vehicleActions) {
			if (a.getType() == ActionType.PICKUP) {
				runningLoad += a.getCurrentTask().weight;
			} else if (a.getType() == ActionType.DELIVER) {
				runningLoad -= a.getCurrentTask().weight;
			}
			if (runningLoad > v.capacity()) {
				return false;
			}
		}
		
		return true;
	}
	
	// Compute the remaining capacity of a vehicle right before performing action with given sequence number
	private int computeRemainingCapacityAtAction(Vehicle v, List<CentralizedAction> vehicleActions, int actionSequenceNumber) {
		int remainingCapacity = v.capacity();
		int runningSequenceNumber = 0;
		for (CentralizedAction a : vehicleActions) {
			if (runningSequenceNumber == actionSequenceNumber) {
				return remainingCapacity;
			}
			if (a.getType() == ActionType.PICKUP) {
				remainingCapacity -= a.getCurrentTask().weight;
			} else if (a.getType() == ActionType.DELIVER) {
				remainingCapacity += a.getCurrentTask().weight;
			}
			runningSequenceNumber ++;
		}
		
		return remainingCapacity;
	}
	
	// Given a list of CentralizedAction objects build the corresponding logist plan
	public Plan buildPlanFromActionList(LinkedList<CentralizedAction> actions, City initialCity) {
		City currentLocation = initialCity;
		Plan p = new Plan(initialCity);
		for (CentralizedAction a : actions) {
			Task t = a.getCurrentTask();
			if (a.getType() == ActionType.PICKUP) {
				// For every pickup action, append move actions to the pickup city
				for (City c : currentLocation.pathTo(t.pickupCity)) {
					p.appendMove(c);
				}
				// update current location
				currentLocation = t.pickupCity;
				// append pickup action
				p.appendPickup(a.getCurrentTask());
			} else if (a.getType() == ActionType.DELIVER) {
				// For every deliver action, append move actions to the deliver city
				for (City c : currentLocation.pathTo(t.deliveryCity)) {
					p.appendMove(c);
				}
				// update current location
				currentLocation = t.deliveryCity;
				// append deliver action
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
	
	// Perform deep copy of the current solution
	public Solution solutionDeepCopy() {
		Solution copy = new Solution(this.vehicles, this.tasks.clone());
		copy.setActions(this.mapDeepCopy(this.actions));
		HashMap<Vehicle, Plan> plansCopy = new HashMap<Vehicle, Plan>();
		for (Map.Entry<Vehicle, Plan> entry : this.plans.entrySet()) {
			plansCopy.put(entry.getKey(), entry.getValue());
		}
		copy.setPlans(plansCopy);
		return copy;
	}
	
	// Given list of vehicles, find the one with largest capacity
	private Vehicle findBiggestVehicle(List<Vehicle> vehicles) {
		Vehicle biggestVehicle = vehicles.get(0);
		for (Vehicle v : this.vehicles) {
			if (biggestVehicle == null || biggestVehicle.capacity() < v.capacity()) {
				biggestVehicle = v;
			}
		}
		return biggestVehicle;
	}
	
	// Compute total cost for all vehicles' plans
	public double computeCost() {
		int finalCost = 0;
		for (Vehicle vehicle : this.vehicles) {
			List<CentralizedAction> vehicleActions = this.actions.get(vehicle);
			finalCost += computeCostForVehicle(vehicleActions, vehicle.getCurrentCity(), vehicle.costPerKm());
		}
		return finalCost;
	}
	
	// Compute cost for a given vehicle's plan
	private double computeCostForVehicle(List<CentralizedAction> vehicleActions, City initialCity, int costPerKm) {
		double cost = 0;
		City currentCity = initialCity;
		for (CentralizedAction action : vehicleActions) {
			City nextCity = null;
			if (action.getType() == ActionType.PICKUP) {
				nextCity = action.getCurrentTask().pickupCity;
			} else {
				nextCity = action.getCurrentTask().deliveryCity;
			}
			double distance = currentCity.distanceTo(nextCity);
			currentCity = nextCity;
			cost += distance * costPerKm;
		}
		return cost;
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

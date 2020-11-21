package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;
import template.DecentralizedAction.ActionType;

public class Solution {
	
	private List<Vehicle> vehicles;
	private HashMap<Vehicle, Plan> plans;
	private HashMap<Vehicle, LinkedList<DecentralizedAction>> actions;
	private ArrayList<Task> tasks;
	
	public ArrayList<Task> getTasks() {
		return this.tasks;
	}
	
	// A solution constructor for the Decentralized MAS
	public Solution(List<Vehicle> vehicles) {
		this.plans = new HashMap<Vehicle, Plan>();	
		this.actions = new HashMap<Vehicle, LinkedList<DecentralizedAction>>();
		for (Vehicle v : vehicles) {
			this.actions.put(v, new LinkedList<DecentralizedAction>());
		}
		this.vehicles = vehicles;
	}
	
	public Solution(List<Vehicle> vehicles, ArrayList<Task> tasks) {
		this.plans = new HashMap<Vehicle, Plan>();	
		this.actions = new HashMap<Vehicle, LinkedList<DecentralizedAction>>();
		for (Vehicle v : vehicles) {
			this.actions.put(v, new LinkedList<DecentralizedAction>());
		}
		this.vehicles = vehicles;
		this.tasks = new ArrayList<Task>(tasks);
	}
		
	public Solution(List<Vehicle> vehicles, TaskSet tasks) {
		this.plans = new HashMap<Vehicle, Plan>();	
		this.actions = new HashMap<Vehicle, LinkedList<DecentralizedAction>>();
		for (Vehicle v : vehicles) {
			this.actions.put(v, new LinkedList<DecentralizedAction>());
		}
		this.vehicles = vehicles;
	}
	
	// Initial solution which assigns every task to a large enough random vehicle
	public Solution createRandomInitialSolution() {
		for (Task t : this.tasks) {
			Random rand = new Random();
			Vehicle randomVehicle = this.vehicles.get(rand.nextInt(this.vehicles.size()));
			while (randomVehicle.capacity() < t.weight) {
				randomVehicle = this.vehicles.get(rand.nextInt(this.vehicles.size()));
			}
			LinkedList<DecentralizedAction> actionsSoFar = this.actions.get(randomVehicle);
			actionsSoFar.add(new DecentralizedAction(t, ActionType.PICKUP));
			actionsSoFar.add(new DecentralizedAction(t, ActionType.DELIVER));
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
		Vehicle biggestVehicle = Utils.findBiggestVehicle(this.vehicles);
		
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
		
		LinkedList<DecentralizedAction> vehicleActions = new LinkedList<DecentralizedAction>();
		for (Task t : initialSolution.tasks) {
			DecentralizedAction pickupAction = new DecentralizedAction(t, ActionType.PICKUP);
			DecentralizedAction deliverAction = new DecentralizedAction(t, ActionType.DELIVER);
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
				initialSolution.actions.put(v, new LinkedList<DecentralizedAction>());
			}
		}
		
		return initialSolution;
	}
	
	
	// Move firrst task of the 'from' vehicle to any other vehicle possible
	private List<Solution> changeFirstTaskAgent(Vehicle from) {
		List<Solution> neighbourSolutions = new ArrayList<Solution>();
		HashMap<Vehicle, LinkedList<DecentralizedAction>> allActions = mapDeepCopy(this.actions);

		// get the first task to be picked up by the 'from' vehicle
		LinkedList<DecentralizedAction> vehicleActions = allActions.get(from);
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
					HashMap<Vehicle, LinkedList<DecentralizedAction>> newSolutionActions = mapDeepCopy(this.actions);
					LinkedList<DecentralizedAction> newActions = new LinkedList<DecentralizedAction>(newSolutionActions.get(to));
					// add pickup & delivery of the new task at the beginning of the vehicle's actions
					newActions.addFirst(new DecentralizedAction(taskToChange, ActionType.DELIVER));
					newActions.addFirst(new DecentralizedAction(taskToChange, ActionType.PICKUP));
					newSolutionActions.put(to, newActions);			
					newSolutionActions.put(from, vehicleActions);
					
					// create a new solution and add to the list of neighbour solutions
					Solution solution = new Solution(this.vehicles, this.tasks);
					solution.setActions(newSolutionActions);
					neighbourSolutions.add(solution);
				}
			}
		}
		
		return neighbourSolutions;
	}
	
	
	private List<Solution> shiftPickupAction(Vehicle v, int actionSequenceNumber) {
		List<Solution> neighbourSolutions = new LinkedList<Solution>();
		List<DecentralizedAction> oldActions = new LinkedList<DecentralizedAction>(this.actions.get(v));
		
		DecentralizedAction taskToMove = oldActions.get(actionSequenceNumber);
		
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
			LinkedList<DecentralizedAction> newSolutionActions = moveActionFromTo(oldActions, actionSequenceNumber, moveForwardTo);
			// make sure capacity constraint is not violated
			if (isCapacityEnough(v, newSolutionActions)) {
				HashMap<Vehicle, LinkedList<DecentralizedAction>> allActions = mapDeepCopy(this.actions);
				allActions.put(v, newSolutionActions);
				Solution solution = new Solution(this.vehicles, this.tasks);
				solution.setActions(allActions);
				neighbourSolutions.add(solution);
			}
			moveForwardTo++;
		}
		
		// shift pickup action to the left
		int moveBackwardsTo = actionSequenceNumber - 1;
		while (moveBackwardsTo >= 0) {
			// update actions list
			LinkedList<DecentralizedAction> newSolutionActions = moveActionFromTo(oldActions, actionSequenceNumber, moveBackwardsTo);
			// make sure capacity constraint is not violated
			if (isCapacityEnough(v, newSolutionActions)) {
				HashMap<Vehicle, LinkedList<DecentralizedAction>> allActions = mapDeepCopy(this.actions);
				allActions.put(v, newSolutionActions);
				Solution solution = new Solution(this.vehicles, this.tasks);
				solution.setActions(allActions);
				neighbourSolutions.add(solution);
			}
			moveBackwardsTo--;
		}
		
		return neighbourSolutions;
	}
	
	private List<Solution> shiftDeliverAction(Vehicle v, int actionSequenceNumber) {
		List<Solution> neighbourSolutions = new LinkedList<Solution>();
		List<DecentralizedAction> oldActions = new LinkedList<DecentralizedAction>(this.actions.get(v));
		
		DecentralizedAction taskToMove = oldActions.get(actionSequenceNumber);
		
		if (actionSequenceNumber < 0 || actionSequenceNumber >= oldActions.size()) {
			return neighbourSolutions;
		}
		
		// shift pickup action to the right
		int moveForwardTo = actionSequenceNumber + 1;	
		while (moveForwardTo < oldActions.size()) {
			// update list of actions
			LinkedList<DecentralizedAction> newSolutionActions = moveActionFromTo(oldActions, actionSequenceNumber, moveForwardTo);
			// make sure capacity constraint is not violated
			if (isCapacityEnough(v, newSolutionActions)) {
				HashMap<Vehicle, LinkedList<DecentralizedAction>> allActions = mapDeepCopy(this.actions);
				allActions.put(v, newSolutionActions);
				Solution solution = new Solution(this.vehicles, this.tasks);
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
			LinkedList<DecentralizedAction> newSolutionActions = moveActionFromTo(oldActions, actionSequenceNumber, moveBackwardsTo);
			// make sure capacity constraint is not violated
			if (isCapacityEnough(v, newSolutionActions)) {
				HashMap<Vehicle, LinkedList<DecentralizedAction>> allActions = mapDeepCopy(this.actions);
				allActions.put(v, newSolutionActions);
				Solution solution = new Solution(this.vehicles, this.tasks);
				solution.setActions(allActions);
				neighbourSolutions.add(solution);
			}
			moveBackwardsTo--;
		}
		
		return neighbourSolutions;

	}
	
	// HELPERS
	
	// Given a list of actions, move action at sequence number "from" to sequence number "to" 
	private LinkedList<DecentralizedAction> moveActionFromTo(List<DecentralizedAction> actions, int from, int to) {
		LinkedList<DecentralizedAction> updatedActions = new LinkedList<DecentralizedAction>(actions);
		
		DecentralizedAction toMove = updatedActions.remove(from);
		int moveToIdx = from < to ? (to - 1) : to;
		updatedActions.add(moveToIdx, toMove);

		return updatedActions;
	}
	
	// Compute the remaining capacity of a vehicle right before performing action with given sequence number
	private boolean isCapacityEnough(Vehicle v, List<DecentralizedAction> vehicleActions) {
		int runningLoad = 0;
		for (DecentralizedAction a : vehicleActions) {
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
		
	// Given a list of DecentralizedAction objects build the corresponding logist plan
	public Plan buildPlanFromActionList(LinkedList<DecentralizedAction> actions, City initialCity) {
		City currentLocation = initialCity;
		Plan p = new Plan(initialCity);
		for (DecentralizedAction a : actions) {
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
	private HashMap<Vehicle, LinkedList<DecentralizedAction>> mapDeepCopy(HashMap<Vehicle, LinkedList<DecentralizedAction>> map) {
		HashMap<Vehicle, LinkedList<DecentralizedAction>> copy = new HashMap<Vehicle, LinkedList<DecentralizedAction>>();
		for (Map.Entry<Vehicle, LinkedList<DecentralizedAction>> entry : map.entrySet()) {
			copy.put(entry.getKey(), new LinkedList<DecentralizedAction>(entry.getValue()));
		}
		
		return copy;
	}
	
	// Perform deep copy of the current solution
	public Solution solutionDeepCopy() {
		Solution copy = new Solution(this.vehicles, this.tasks);
		copy.setActions(this.mapDeepCopy(this.actions));
		HashMap<Vehicle, Plan> plansCopy = new HashMap<Vehicle, Plan>();
		for (Map.Entry<Vehicle, Plan> entry : this.plans.entrySet()) {
			plansCopy.put(entry.getKey(), entry.getValue());
		}
		copy.setPlans(plansCopy);
		return copy;
	}
	
	// Compute total cost for all vehicles' plans
	public double computeCost() {
		int finalCost = 0;
		for (Vehicle vehicle : this.vehicles) {
			List<DecentralizedAction> vehicleActions = this.actions.get(vehicle);
			finalCost += computeCostForVehicle(vehicleActions, vehicle.getCurrentCity(), vehicle.costPerKm());
		}
		return finalCost;
	}
	
	// Compute cost for a given vehicle's plan
	private double computeCostForVehicle(List<DecentralizedAction> vehicleActions, City initialCity, int costPerKm) {
		double cost = 0;
		City currentCity = initialCity;
		for (DecentralizedAction action : vehicleActions) {
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

	public HashMap<Vehicle, LinkedList<DecentralizedAction>> getActions() {
		return actions;
	}

	public void setActions(HashMap<Vehicle, LinkedList<DecentralizedAction>> actions) {
		this.actions = actions;
	}
}
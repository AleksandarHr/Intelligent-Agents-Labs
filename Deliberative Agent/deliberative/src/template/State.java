package template;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;
import logist.topology.Topology.City;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class State {

	private City currentLocation;
	private TaskSet runningTasks;
	private TaskSet remainingTasks;
	private int remainingCapacity;
	private double currentCost;
	public List<Action> pastActions;
	private List<State> successorStates;
	private Vehicle vehicle;
	// previous can be used for cycle detection
	private State previous;
	private Plan runningPlan;

	public State(Vehicle v, TaskSet initialTasks) {
		this.vehicle = v;
		this.currentLocation = v.getCurrentCity();
		this.remainingTasks = initialTasks.clone();
		this.runningTasks = TaskSet.noneOf(initialTasks); // ?? trying to create an empty TaskSet
		this.runningPlan = new Plan(this.currentLocation);
		this.remainingCapacity = vehicle.capacity();
	}
	
	public State () {
		
	}
	
	public State(City currentCity) {
		this.currentLocation = currentCity;
		this.runningTasks = null;
		this.remainingTasks = null;
		this.pastActions = new ArrayList<Action>();
		this.currentCost = 0.0;
		this.remainingCapacity = 0;
		this.successorStates = new ArrayList<State>();
		this.runningPlan = new Plan(currentCity);
		this.previous = null;
	}

	public State(Vehicle vehicle, TaskSet tasks, TaskSet carriedTasks) {
		this.vehicle = vehicle;
		this.currentLocation = vehicle.getCurrentCity();
		// Tasks left to be picked up on the map
		this.remainingTasks = tasks.clone();
		// Current carried tasks by a vehicle
		this.runningTasks = carriedTasks.clone();
		this.remainingCapacity = vehicle.capacity();
		this.currentCost = 0;
		this.pastActions = new ArrayList<Action>();
		this.previous = null;
	}

	public List<State> generateSuccessorStates() {
		List<State> successorStates = new ArrayList<State>();
		
		// Generate successor states after a move action
		for (City neighbour : currentLocation.neighbors()) {
			State next = duplicateState();
			next.setCurrentLocation(neighbour);
			next.increaseCost(this.vehicle.costPerKm() * currentLocation.distanceTo(neighbour));
			next.runningPlan.appendMove(neighbour);
			successorStates.add(next);
		}

		// Generate successor states after a pickup action
		List<Task> currentCityPickupTasks = getRemainingTasksInCurrentCity();
		if (currentCityPickupTasks.size() != 0) {
			for (Task t : currentCityPickupTasks) {
				State next = duplicateState();
				next.pickupTask(t);
				next.runningPlan.appendPickup(t);
				successorStates.add(next);
			}
		}		

		// Generate successor states after a delivery action
		List<Task> currentCityDeliveryTasks = getRunningTasksForCurrentCity();
		if (currentCityDeliveryTasks.size() != 0) {
			for (Task t : currentCityDeliveryTasks) {
				State next = duplicateState();
				next.deliverTask(t);
				next.runningPlan.appendDelivery(t);
				successorStates.add(next);
			}
		}
		
		return successorStates;
	}

	public void increaseCost(double additionalCost) {
		this.currentCost += additionalCost;
	}

	public void pickupTask(Task t) {
		this.runningTasks.add(t);
		this.remainingTasks.remove(t);
		this.remainingCapacity -= t.weight;
	}
	
	public void deliverTask(Task t) {
		this.runningTasks.remove(t);
		this.remainingCapacity += t.weight;
	}

	public List<Task> getRemainingTasksInCurrentCity() {
		List<Task> currentCityTasks = new LinkedList<Task>();
		for (Task t : this.remainingTasks) {
			if (t.pickupCity.equals(this.currentLocation) && t.weight <= this.remainingCapacity) {
				currentCityTasks.add(t);
			}
		}
		return currentCityTasks;
	}

	public List<Task> getRunningTasksForCurrentCity() {
		List<Task> currentCityTasks = new LinkedList<Task>();
		for (Task t : this.runningTasks) {
			if (t.deliveryCity.equals(this.currentLocation)) {
				currentCityTasks.add(t);
			}
		}
		return currentCityTasks;
	}

	public State duplicateState() {
		State dupState = new State();
		dupState.setCurrentLocation(this.currentLocation);
		dupState.setCurrentCost(this.currentCost);
		dupState.setCurrentTasks(this.runningTasks.clone());
		dupState.setPastActions(this.pastActions);
		dupState.setRemainingCapacity(this.remainingCapacity);
		dupState.setRemainingTasks(this.remainingTasks.clone());
		dupState.setVehicle(this.vehicle);
		dupState.setPreviousState(this);
		dupState.setPlan(this.runningPlan);
		return dupState;
	}

	public boolean discovered(State other) {	
		List<Boolean> checks = new ArrayList<Boolean>();
		// Is in the same city
		checks.add(this.currentLocation.equals(other.currentLocation));
		// Carrying the same tasks
		checks.add(this.runningTasks.equals(other.runningTasks));
		// Having completed the same deliveries
		checks.add(this.remainingTasks.equals(other.remainingTasks));
		// Having equal or greater cost
		checks.add(this.currentCost >= other.currentCost);

		for (Boolean check : checks) {
			if (!check) {
				return false;
			}
		}
		return true;
	}

	public boolean isStateFinal() {
		return (this.remainingTasks.isEmpty() && this.runningTasks.isEmpty());
	}
	
	public boolean hasCycle(State s) {
		State prev = s.previous;
		while (prev != null) {
			if (prev.equals(s)) {
				return true;
			}
		}
		return false;
	}
	
	// GETTERS & SETTERS
	public void setCurrentLocation(City currentLocation) {
		this.currentLocation = currentLocation;
	}

	public void setCurrentTasks(TaskSet currentTasks) {
		this.runningTasks = currentTasks;
	}

	public void setRemainingTasks(TaskSet remainingTasks) {
		this.remainingTasks = remainingTasks;
	}

	public void setRemainingCapacity(int remainingCapacity) {
		this.remainingCapacity = remainingCapacity;
	}

	public void setCurrentCost(double currentCost) {
		this.currentCost = currentCost;
	}

	public void setPastActions(List<Action> pastActions) {
		this.pastActions = pastActions;
	}

	public void setSuccessorStates(List<State> successorStates) {
		this.successorStates = successorStates;
	}

	public void setVehicle(Vehicle vehicle) {
		this.vehicle = vehicle;
	}

	public List<State> getSuccessorStates() {
		return this.successorStates;
	}

	public double getCost() {
		return this.currentCost;
	}

	public City getCurrentLocation() {
		return this.currentLocation;
	}
	
	public State getPreviousState() {
		return this.previous;
	}

	public State setPreviousState(State s) {
		return this.previous = s;
	}
	
	public Plan getPlan() {
		return this.runningPlan;
	}

	public void setPlan(Plan p) {
		this.runningPlan = p;
	}
}

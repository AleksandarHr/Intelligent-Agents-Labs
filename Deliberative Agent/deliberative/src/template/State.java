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
	private TaskSet currentTasks;
	private TaskSet remainingTasks;
	private int remainingCapacity;
	private double currentCost;
	public List<Action> pastActions;
	private List<State> successorStates;
	private Vehicle vehicle;
	public Plan runningPlan;

	public State(City currentCity) {
		this.currentLocation = currentCity;
		this.currentTasks = null;
		this.remainingTasks = null;
		this.pastActions = new ArrayList<Action>();
		this.currentCost = 0.0;
		this.remainingCapacity = 0;
		this.successorStates = new ArrayList<State>();
		this.runningPlan = new Plan(currentCity);
	}

	public State(Vehicle vehicle, TaskSet tasks, TaskSet carriedTasks) {
		this.vehicle = vehicle;
		this.currentLocation = vehicle.getCurrentCity();
		// Tasks left to be picked up on the map
		this.remainingTasks = tasks.clone();
		// Current carried tasks by a vehicle
		this.currentTasks = carriedTasks.clone();
		this.remainingCapacity = vehicle.capacity();
		this.currentCost = 0;
		this.pastActions = new ArrayList<>();
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

	public void generateSuccessorStates() {
		List<State> successorStates = new ArrayList<State>();
		for (City neighbour : currentLocation.neighbors()) {
			State next = duplicateState();
			next.setCurrentLocation(neighbour);
			next.increaseCost(this.vehicle.costPerKm() * currentLocation.distanceTo(neighbour));
			next.runningPlan.appendMove(neighbour);
			successorStates.add(next);
		}

		// Generate successor states after a pickup action
		List<Task> currentCityPickupTasks = getRemainingTasksPickupCities();
		if (currentCityPickupTasks.size() != 0) {
			for (Task t : currentCityPickupTasks) {
				State next = duplicateState();
				next.pickupTask(t);
				next.runningPlan.appendPickup(t);
				successorStates.add(next);
			}
		}

		// Generate successor states after a pickup action
		List<Task> currentCityDeliveryTasks = getRemainingTasksDeliveryCities();
		if (currentCityDeliveryTasks.size() != 0) {
			for (Task t : currentCityDeliveryTasks) {
				State next = duplicateState();
				next.deliverTask(t);
				next.runningPlan.appendDelivery(t);
				successorStates.add(next);
			}
		}
	}

	public void increaseCost(double additionalCost) {
		this.currentCost += additionalCost;
	}

	public void pickupTask(Task t) {
		this.currentTasks.add(t);
		this.remainingTasks.remove(t);
		this.remainingCapacity -= t.weight;
	}
	
	public void deliverTask(Task t) {
		this.currentTasks.remove(t);
		this.remainingCapacity += t.weight;
	}

	public List<Task> getRemainingTasksPickupCities() {
		List<Task> currentCityTasks = new LinkedList<Task>();
		for (Task t : this.remainingTasks) {
			if (t.pickupCity.equals(this.currentLocation) && t.weight <= this.remainingCapacity) {
				currentCityTasks.add(t);
			}
		}
		return currentCityTasks;
	}

	public List<Task> getRemainingTasksDeliveryCities() {
		List<Task> currentCityTasks = new LinkedList<Task>();
		for (Task t : this.remainingTasks) {
			if (t.deliveryCity.equals(this.currentLocation)) {
				currentCityTasks.add(t);
			}
		}
		return currentCityTasks;
	}

	public State duplicateState() {
		State dupState = new State(this.currentLocation);
		dupState.setCurrentCost(this.currentCost);
		dupState.setCurrentTasks(this.currentTasks);
		dupState.setPastActions(this.pastActions);
		dupState.setRemainingCapacity(this.remainingCapacity);
		dupState.setRemainingTasks(this.remainingTasks);
		// dupState.setSuccessorStates(this.successorStates);
		dupState.setVehicle(this.vehicle);

		return dupState;
	}

	public boolean discovered(State other) {
		List<Boolean> checks = new ArrayList<>();
		// Is in the same city
		checks.add(this.currentLocation.equals(other.currentLocation));
		// Carrying the same tasks
		checks.add(this.currentTasks.equals(other.currentTasks));
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

	public void setCurrentLocation(City currentLocation) {
		this.currentLocation = currentLocation;
	}

	public void setCurrentTasks(TaskSet currentTasks) {
		this.currentTasks = currentTasks;
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

}

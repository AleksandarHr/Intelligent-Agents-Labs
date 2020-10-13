package template;

import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.plan.Action.Delivery;
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

	private City startingLocation;
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

	public State(Vehicle v, TaskSet initialTasks) {
		this.startingLocation = v.getCurrentCity();
		this.vehicle = v;
		this.currentLocation = v.getCurrentCity();
		this.remainingTasks = initialTasks.clone();
		this.runningTasks = TaskSet.noneOf(initialTasks); // ?? trying to create an empty TaskSet
		this.remainingCapacity = vehicle.capacity();
		this.pastActions = new ArrayList<Action>();
		this.previous = null;
	}
	
	public State (City city, double cost, TaskSet running, TaskSet remaining, List<Action> actions, 
			int remainingCapacity, Vehicle v, State previous) {
		this.currentLocation = city;
		this.currentCost = cost;
		this.runningTasks = running;
		this.remainingTasks = remaining;
		this.pastActions = new ArrayList<Action>(actions);
		this.remainingCapacity = remainingCapacity;
		this.vehicle = v;
		this.previous = previous;
	}
	
	public List<State> generateSuccessorStates() {
		List<State> successorStates = new ArrayList<State>();
		
		// Generate successor states after a move action
		for (City neighbour : currentLocation.neighbors()) {
			State next = duplicateState();
			next.setCurrentLocation(neighbour);
			next.increaseCost(this.vehicle.costPerKm() * currentLocation.distanceTo(neighbour));
			if (!next.hasCycle()) {
				next.pastActions.add(new Move(neighbour));
				successorStates.add(next);
			}
		}

		// Generate successor states after a pickup action
		List<Task> currentCityPickupTasks = getRemainingTasksInCurrentCity();
		for (Task t : currentCityPickupTasks) {
			State next = duplicateState();
			next.pickupTask(t);
			if (!next.hasCycle()) {
				next.pastActions.add(new Pickup(t));
				successorStates.add(next);		
			}
		}

		// Generate successor states after a delivery action
		List<Task> currentCityDeliveryTasks = getRunningTasksForCurrentCity();
		for (Task t : currentCityDeliveryTasks) {
			State next = duplicateState();
			next.deliverTask(t);
			if (!next.hasCycle()) {
				next.pastActions.add(new Delivery(t));
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

	private State duplicateState() {
		State dupState = new State(this.currentLocation, this.currentCost, this.runningTasks.clone(),
				this.remainingTasks.clone(), this.pastActions, this.remainingCapacity, this.vehicle, this);
		return dupState;
	}

	public boolean isStateRedundant(List<State> visited) {
		for (State c : visited) {
			if (this.discovered(c)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean discovered(State other) {	
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
	
	public boolean hasCycle() {
		State prev = this.previous;
		while (prev != null) {
			if (prev.equals(this)) {
				return true;
			}
			prev = prev.getPreviousState();
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
		Plan p = new Plan(this.startingLocation);
		for (Action a : this.pastActions) {
			p.append(a);
		}
		return p;
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currentLocation == null) ? 0 : currentLocation.hashCode());
		result = prime * result + ((remainingTasks == null) ? 0 : remainingTasks.hashCode());
		result = prime * result + ((runningTasks == null) ? 0 : runningTasks.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		State other = (State) obj;
		if (currentLocation == null) {
			if (other.currentLocation != null)
				return false;
		} else if (!currentLocation.equals(other.currentLocation))
			return false;
		if (remainingTasks == null) {
			if (other.remainingTasks != null)
				return false;
		} else if (!remainingTasks.equals(other.remainingTasks))
			return false;
		if (runningTasks == null) {
			if (other.runningTasks != null)
				return false;
		} else if (!runningTasks.equals(other.runningTasks))
			return false;
		return true;
	}

}

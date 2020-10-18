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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

	/*
	 * Generates all successor states of the current one
	 */
	public List<State> generateSuccessorStates() {
		List<State> successorStates = new ArrayList<State>();
		Set<City> citiesOfInterest = new HashSet<City>();
		
		// For all tasks remaining to be picked up
		List<Task> currentCityPickupTasks = getRemainingTasksInCurrentCity();
		for (Task t : this.remainingTasks) {
			// If the task is in current city, pick it up
			if (currentCityPickupTasks.contains(t)) {
				State next = duplicateState();
				next.pickupTask(t);
				if (!next.hasCycle()) {
					next.pastActions.add(new Pickup(t));
					successorStates.add(next);		
				}
			} else {
				// if not, add the neigbhour city on the path to its pickup city (e.g. the first one)
				//		to the set of interesting cities
				List<City> path = this.currentLocation.pathTo(t.pickupCity);
				citiesOfInterest.add(path.get(0));
			}
		}

		// Generate successor states after a delivery action
		List<Task> currentCityDeliveryTasks = getRunningTasksForCurrentCity();
		for (Task t : this.runningTasks) {
			// If the task is to be delivered in the current city, deliver it
			if (currentCityDeliveryTasks.contains(t)) {
				State next = duplicateState();
				next.deliverTask(t);
				if (!next.hasCycle()) {
					next.pastActions.add(new Delivery(t));
					successorStates.add(next);
				}
			} else {
				// if not, add the neigbhour city on the path to its delivery city (e.g. the first one)
				//		to the set of interesting cities
				List<City> path = this.currentLocation.pathTo(t.deliveryCity);
				citiesOfInterest.add(path.get(0));
			}
		}
		
		// Perform a move action to each of the cities of interest and create a successor state
		for (City c : citiesOfInterest) {
			if (currentLocation.neighbors().contains(c)) {
				State next = duplicateState();
				next.setCurrentLocation(c);
				next.increaseCost(this.vehicle.costPerKm() * currentLocation.distanceTo(c));
				if (!next.hasCycle()) {
					next.pastActions.add(new Move(c));
					successorStates.add(next);
				}
			}
		}
		
		return successorStates;
	}
	
	public void increaseCost(double additionalCost) {
		this.currentCost += additionalCost;
	}

	/*
	 * Given a task t, removes it from the agent's remaining tasks, adds it
	 * to the agent's running tasks and decreases the agent's remaining capacity
	 */
	public void pickupTask(Task t) {
		this.runningTasks.add(t);
		this.remainingTasks.remove(t);
		this.remainingCapacity -= t.weight;
	}
	
	/*
	 * Given a task t, removes it from the agent's running tasks and increases
	 * the agent's remaining capacity
	 */
	public void deliverTask(Task t) {
		this.runningTasks.remove(t);
		this.remainingCapacity += t.weight;
	}

	/*
	 * Returns a list of tasks which the agent has not yet picked up and which have
	 * the same pickup city as the agent's current location
	 */
	public List<Task> getRemainingTasksInCurrentCity() {
		List<Task> currentCityTasks = new LinkedList<Task>();
		for (Task t : this.remainingTasks) {
			if (t.pickupCity.equals(this.currentLocation) && t.weight <= this.remainingCapacity) {
				currentCityTasks.add(t);
			}
		}
		return currentCityTasks;
	}

	/*
	 * Returns a list of tasks which the agent has already picked up but has not delivered
	 *  and which have the same delivery city as the agent's current location
	 */
	public List<Task> getRunningTasksForCurrentCity() {
		List<Task> currentCityTasks = new LinkedList<Task>();
		for (Task t : this.runningTasks) {
			if (t.deliveryCity.equals(this.currentLocation)) {
				currentCityTasks.add(t);
			}
		}
		return currentCityTasks;
	}

	/*
	 * Returns a State object which is a duplicate of the current State object
	 */
	private State duplicateState() {
		State dupState = new State(this.currentLocation, this.currentCost, this.runningTasks.clone(),
				this.remainingTasks.clone(), this.pastActions, this.remainingCapacity, this.vehicle, this);
		return dupState;
	}

	/*
	 * Given a listed of already visited states, checks if the current state has been discovered
	 */
	public boolean isStateRedundant(List<State> visited) {
		for (State c : visited) {
			if (this.discovered(c)) {
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Returns true if the current state has been previously reached with a lower cost
	 */
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

	/*
	 * Returns true if both remainigTasks and runningTasks sets are empty
	 */
	public boolean isStateFinal() {
		return (this.remainingTasks.isEmpty() && this.runningTasks.isEmpty());
	}
	
	/*
	 * Traverses all previous states, starting from the curernt one, and checks
	 * for cycles along the path.
	 */
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
	
	/*
	 * Build a plan based on the current state's past actions and return it
	 */
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

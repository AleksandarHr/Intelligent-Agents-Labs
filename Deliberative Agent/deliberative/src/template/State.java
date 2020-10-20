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
import java.util.Collections;
import java.util.Comparator;
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
	private List<State> successorStates;
	private Vehicle vehicle;
	private double heuristics;

	public State(Vehicle v, TaskSet initialTasks) {
		this.startingLocation = v.getCurrentCity();
		this.vehicle = v;
		this.currentLocation = v.getCurrentCity();
		this.remainingTasks = initialTasks.clone();
		this.runningTasks = TaskSet.noneOf(initialTasks); //create an empty TaskSet
		this.remainingCapacity = vehicle.capacity();
	}
	
	public State (City city, TaskSet running, TaskSet remaining, 
			int remainingCapacity, Vehicle v) {
		this.currentLocation = city;
		this.runningTasks = running;
		this.remainingTasks = remaining;
		this.remainingCapacity = remainingCapacity;
		this.vehicle = v;

	}

	/*
	 * Generates all successor states of the current one
	 */
	//Used to sort tasks which can be delivered, in case multiple tasks can be delivered in one city
	Comparator<Task> meinComparator = (Task t0, Task t1) -> Integer.compare(t0.id, t1.id);
	
	public List<State> generateSuccessorStates() {
		List<State> successorStates = new ArrayList<State>();
		List<Task> sortedTasks = new ArrayList<Task>(this.runningTasks);
		sortedTasks.sort(meinComparator);
		for (Task task : sortedTasks) {
			State n = duplicateState();
			n.deliverTask(task);
			n.currentLocation = task.deliveryCity;
			//If we are in the delivery city, deliver the first task
			if (task.deliveryCity.equals(currentLocation)) {
				return Arrays.asList(n);
			}
			successorStates.add(n);
		}

		for (Task task : this.remainingTasks) {
			if(task.weight <= this.remainingCapacity) {
				State n = duplicateState();
				n.pickupTask(task);
				n.currentLocation = task.pickupCity;
				successorStates.add(n);
			}
		}
		
		return successorStates;
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
	 * Returns a State object which is a duplicate of the current State object
	 */
	private State duplicateState() {
		State dupState = new State(this.currentLocation, this.runningTasks.clone(),
				this.remainingTasks.clone(), this.remainingCapacity, this.vehicle);
		return dupState;
	}

	public double getHeuristics() {
		return heuristics;
	}

	/*
	 * Calculate heuristics, the longest possible path to finishing a task
	 */
	public Double computeHeuristics() {
		List<Double> list = new LinkedList<Double>();
		double max = 0.0;
		for (Task task : this.remainingTasks) {
			list.add(this.currentLocation.distanceTo(task.pickupCity) + task.pickupCity.distanceTo(task.deliveryCity));
		}

		for (Task task : this.runningTasks) {
			list.add(this.currentLocation.distanceTo(task.deliveryCity));
		}
		if (list.isEmpty()) {
			this.heuristics = 0.0;
			return 0.0;
		}
		max = Collections.max(list);
		this.heuristics = max;
		return max;
	}
	

	/*
	 * Returns true if both remainigTasks and runningTasks sets are empty
	 */
	public boolean isStateFinal() {
		return (this.remainingTasks.isEmpty() && this.runningTasks.isEmpty());
	}
	

	// GETTERS & SETTERS
	public void setCurrentLocation(City currentLocation) {
		this.currentLocation = currentLocation;
	}

	public void setRunningTasks(TaskSet currentTasks) {
		this.runningTasks = currentTasks;
	}
	
	public TaskSet getRunningTasks() {
		return this.runningTasks;
	}
	
	public TaskSet getRemainingTasks() {
		return this.remainingTasks;
	}

	public void setRemainingTasks(TaskSet remainingTasks) {
		this.remainingTasks = remainingTasks;
	}

	public void setRemainingCapacity(int remainingCapacity) {
		this.remainingCapacity = remainingCapacity;
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
	
	public Vehicle getVehicle() {
		return this.vehicle;
	}

	public City getCurrentLocation() {
		return this.currentLocation;
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

	//Helper functions which compares 2 tasksets
	private boolean compareTaskSet(TaskSet t1, TaskSet t2) {
		Set<Integer> hashSetIDs = new HashSet<Integer>();
		if(t1.size() != t2.size()) {
			return false;
		}
		for(Task task : t1) {
			hashSetIDs.add(task.id);
		}
		for(Task task : t2) {
			if(!hashSetIDs.contains(task.id)) {
				return false;
			}
		}
		return true;
	}
	//Used for hashlist
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
		} else if (!compareTaskSet(remainingTasks, other.remainingTasks))
			return false;
		if (runningTasks == null) {
			if (other.runningTasks != null)
				return false;
		} else if (!compareTaskSet(runningTasks, other.runningTasks))
			return false;
		return true;
	}

}

package template;

import java.util.List;

import logist.simulation.Vehicle;
import logist.task.TaskSet;

public class StochasticLocalSearch {
	
	List<Vehicle> vehicles;
	TaskSet tasks;
	public StochasticLocalSearch(List<Vehicle> vehicles, TaskSet tasks) {
		super();
		this.vehicles = vehicles;
		this.tasks = tasks;
	}

}

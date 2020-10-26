package template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import logist.plan.Plan;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskSet;

public class Solution {
	
	private List<Vehicle> vehicles;
	private HashMap<Vehicle, Plan> plans;
	private TaskSet tasks;
	
	// Maps an action to the vehicle which performs it
	private HashMap<CentralizedAction, Vehicle> actionToVehicle;
	// Maps and action to the action following it (both performed by the same vehicle)
	private HashMap<CentralizedAction, CentralizedAction> actionToNextAction;	
	// Maps an action to its sequence number
	private HashMap<CentralizedAction, Integer> actionToSequenceNumber;
	// Maps a vehicle to its sequence number counter
	private HashMap<Vehicle, Integer> vehicleToSequenceCounter;
	
	public Solution(List<Vehicle> vehicles, TaskSet tasks) {
		this.actionToVehicle = new HashMap<CentralizedAction, Vehicle>();
		this.actionToNextAction = new HashMap<CentralizedAction, CentralizedAction>();
		this.actionToSequenceNumber = new HashMap<CentralizedAction, Integer>();
		this.vehicleToSequenceCounter = new HashMap<Vehicle, Integer>();
		this.plans = new HashMap<Vehicle, Plan>();
		
		// The sequence counter of each vehicle is initialized to 0
		for (Vehicle v : this.vehicles) {
			this.vehicleToSequenceCounter.put(v, 0);
		}
		
		this.vehicles = vehicles;
		this.tasks = tasks;
	}
	
	// Initial solution assigns all tasks to the biggest vehicle available
	public void createInitialSolution() {
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
				return;
			}
		}
		
		Plan initialPlan = buildInitialPlan(biggestVehicle);
		
		this.plans.put(biggestVehicle, initialPlan);
	}
	
	private Plan buildInitialPlan(Vehicle biggestVehicle) {
		Plan p = new Plan(biggestVehicle.getCurrentCity());
		for (Task t : this.tasks) {
			// TODO: Use the biggest vehicle to pickup & deliver all tasks
		}
		
		return p;
	}
}

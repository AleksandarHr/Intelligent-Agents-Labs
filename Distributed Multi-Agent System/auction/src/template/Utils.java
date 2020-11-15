package template;

import java.util.List;

import logist.simulation.Vehicle;

public class Utils {
	// Given list of vehicles, find the one with largest capacity
	public static Vehicle findBiggestVehicle(List<Vehicle> vehicles) {
		Vehicle biggestVehicle = vehicles.get(0);
		for (Vehicle v : vehicles) {
			if (biggestVehicle == null || biggestVehicle.capacity() < v.capacity()) {
				biggestVehicle = v;
			}
		}
		return biggestVehicle;
	}
}

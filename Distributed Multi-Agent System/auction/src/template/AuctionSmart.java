package template;

//the list of imports
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * A very smart auction agent that beats everyone else.
 * 
 */
@SuppressWarnings("unused")
public class AuctionSmart implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

		this.topology = topology;
		this.distribution = distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();

		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			currentCity = previous.deliveryCity;
		}
	}

	@Override
	public Long askPrice(Task task) {

		if (vehicle.capacity() < task.weight)
			return null;

		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask + currentCity.distanceUnitsTo(task.pickupCity);
		double marginalCost = Measures.unitsToKM(distanceSum * vehicle.costPerKm());

		double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
		double bid = ratio * marginalCost;

		return (long) Math.round(bid);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

//		System.out.println("Agent " + agent.id() + " has tasks " + tasks);

		Plan planVehicle1 = naivePlan(vehicle, tasks);

		List<Plan> plans = new ArrayList<Plan>();
		plans.add(planVehicle1);
		while (plans.size() < vehicles.size())
			plans.add(Plan.EMPTY);

		return plans;
	}

	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}

	
	// SLS Stuff from previous lab
	// ===================================
	private List<Plan> slsPlans(List<Vehicle> vehicles, TaskSet tasks, int iterationsBound, double p, long startTime,
			boolean pickRandom) {
		Solution currentBestSolution = new Solution(vehicles, tasks);
		currentBestSolution.createRandomInitialSolution();

		Solution bestSoFar = currentBestSolution;

		int iterationCount = 0;
		// iterate until we reach iterationsBound or we timeout
		while (iterationCount < iterationsBound && timeOut(startTime)) {
			iterationCount++;
			// generate neigbhours of current best solution
			List<Solution> neighbors = currentBestSolution.generateNeighbourSolutions();
			Solution previousBestSolution = currentBestSolution;
			if (Math.random() <= p) {
				// with probability p, proceed with the best neighbor solution
				Solution bestPlan = currentBestSolution != null ? currentBestSolution : neighbors.get(0);
				double bestCost = bestPlan.computeCost();
				// Find the solution with the best plan
				for (Solution plan : neighbors) {
					double cost = plan.computeCost();
					if (cost <= bestCost) {
						bestPlan = plan;
						bestCost = cost;
					}
				}
				currentBestSolution = bestPlan;
			} else {
				// with probability (1-p) either revert back to the previous best solution
				// or pick a random neighbour (depending on boolean argument)
				if (neighbors.size() > 0) {
					if (pickRandom) {
						Random random = new Random();
						int index = random.nextInt(neighbors.size());
						currentBestSolution = neighbors.get(index);
					} else {
						currentBestSolution = previousBestSolution;
					}
				}
			}

			bestSoFar = currentBestSolution.computeCost() < bestSoFar.computeCost() ? currentBestSolution : bestSoFar;
		}

		// Build logist plan for every vehicle from the actions in the best solution
		// found
		List<Plan> optimalVehiclePlans = new ArrayList<Plan>(vehicles.size());
		for (Vehicle v : vehicles) {
			LinkedList<DecentralizedAction> actions = bestSoFar.getActions().get(v);
			Plan plan = bestSoFar.buildPlanFromActionList(actions, v.getCurrentCity());
			optimalVehiclePlans.add(plan);
		}
		return optimalVehiclePlans;
	}

	private boolean timeOut(long startTime) {
		// TODO: Change timeout_plan stuff
		long timeout_plan = 1000;

		long currentTime = System.currentTimeMillis();
		long duration = currentTime - startTime;
		// Half of a second to build a plan
		return duration < timeout_plan - 500;
	}

}

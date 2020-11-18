package template;

import java.io.File;
//the list of imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import logist.LogistSettings;
import logist.Measures;
import logist.behavior.AuctionBehavior;
import logist.config.Parsers;
import logist.agent.Agent;
import logist.simulation.Vehicle;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;
import logist.task.DefaultTaskDistribution;

import template.Utils;

/**
 * A very smart auction agent that beats everyone else.
 * 
 */
@SuppressWarnings("unused")
public class AuctionSmart implements AuctionBehavior {

	private Topology topology;
	private TaskDistribution distribution;
	private DefaultTaskDistribution defaultDistribution;
	private Agent agent;
	private Random random;
	private Vehicle vehicle;
	private City currentCity;
	private ArrayList<Task> taskArray;

	private Solution currentSolution, extendedSolution;
	private HashMap<Integer, List<Long>> agentsBidsHistory;
	private ArrayList<Integer> winCounts;
	private long totalBidsWon = 0;

	private long setupTimeout, planTimeout, bidTimeout;

	private double increaseRate = 0.5;
	private double risk = 0.9;
	int iterationsBound = 10000;
	double p = 0.3;
	long startTime;
	boolean pickRandom = true;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

		this.agentsBidsHistory = new HashMap<Integer, List<Long>>();
		this.winCounts = new ArrayList<Integer>();
		this.taskArray = new ArrayList<Task>();

		this.topology = topology;
		this.distribution = distribution;
		this.defaultDistribution = (DefaultTaskDistribution) this.distribution;
		this.agent = agent;
		this.vehicle = agent.vehicles().get(0);
		this.currentCity = vehicle.homeCity();

		// Read in timeouts for setup, plan, and bid
		LogistSettings ls = null;
		try {
			ls = Parsers.parseSettings("config" + File.separator + "settings_auction.xml");
			this.setupTimeout = ls.get(LogistSettings.TimeoutKey.SETUP);
			this.planTimeout = ls.get(LogistSettings.TimeoutKey.PLAN);
			this.bidTimeout = ls.get(LogistSettings.TimeoutKey.BID);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Unable to parse config file.");
		}

		this.currentSolution = new Solution(agent.vehicles());

		long seed = -9019554669489983951L * currentCity.hashCode() * agent.id();
		this.random = new Random(seed);
	}

	@Override
	public void auctionResult(Task previous, int winner, Long[] bids) {
		if (winner == agent.id()) {
			this.taskArray.add(previous);
			this.currentSolution = extendedSolution.solutionDeepCopy();
			this.totalBidsWon += bids[winner];
		}

		// Update bookkeeping - bid history & agents' win counts
		this.updateBidHistoryAndWinners(bids, winner);
	}

	private void updateBidHistoryAndWinners(Long[] newBids, int winnerId) {
		for (int i = 0; i < newBids.length; i++) {
			this.agentsBidsHistory.computeIfAbsent(i, k -> new LinkedList<Long>());
			this.agentsBidsHistory.get(i).add(newBids[i]);
		}
//		this.winCounts.add(winnerId, this.winCounts.get(winnerId)+1);
	}

	@Override
	public Long askPrice(Task task) {

		Vehicle biggestVehicle = Utils.findBiggestVehicle(this.agent.vehicles());
		if (biggestVehicle.capacity() < task.weight) {
			return null;
		}

		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask + currentCity.distanceUnitsTo(task.pickupCity);

		double marginalCost = computeLessSimpleMarginalCost(task);

		double ratio = 1.0 + (random.nextDouble() * this.increaseRate * task.id);
		double bid = (1.0 + increaseRate) * marginalCost;
		System.out.println("MARGINAL = " + marginalCost + "  ::  BID = " + bid);

		return (long) Math.round(bid);
	}

	// Simple computation of marginal cost - difference between extended solution
	// and current solution
	public double computeLessSimpleMarginalCost(Task taskToAdd) {
		double marginalCost = 0.0;

//		this.currentSolution = this.getBestSlsSolution(agent.vehicles(), this.tasksSoFar, this.iterationsBound, this.p, this.startTime, this.pickRandom);
		ArrayList<Task> extendedTasks = new ArrayList<Task>(this.taskArray);
		extendedTasks.add(taskToAdd);
		this.extendedSolution = this.getBestSlsSolution(agent.vehicles(), extendedTasks, this.iterationsBound, this.p,
				this.startTime, this.pickRandom);
//

		// ADD future prediction task
		Random rand = new Random();
		List<City> cities = this.topology.cities();
		City from = null;
		City to = null;
		double worstMarginalCost = Double.MAX_VALUE;
		int idCounter = 100;
		for (int i = 0; i < 15; i++) {
			ArrayList<Task> futureTasks = new ArrayList<Task>(this.taskArray);
			ArrayList<Task> futureExtendedTasks = new ArrayList<Task>(futureTasks);
			futureExtendedTasks.add(taskToAdd);
//			for (int j = 0; j < 10; j++) {
//				while (from == null || to == null || to == from || this.distribution.probability(from, to) < 0.1) {
//					from = cities.get(rand.nextInt(cities.size()));
//					to = cities.get(rand.nextInt(cities.size()));
//					System.out.println("PROB = " + this.distribution.probability(from, to));
//				}
//				int expectedWeight = this.distribution.weight(from, to);
//				int expectedReward = this.distribution.reward(from, to);
				Task predictedTask = this.defaultDistribution.createTask();
//				idCounter++;
				futureTasks.add(predictedTask);
				futureExtendedTasks.add(predictedTask);
//			}
			Solution tempCurrent = this.getBestSlsSolution(agent.vehicles(), futureTasks, this.iterationsBound, this.p,
					this.startTime, this.pickRandom);
			Solution tempExtended = this.getBestSlsSolution(agent.vehicles(), futureExtendedTasks, this.iterationsBound,
					this.p, this.startTime, this.pickRandom);

			double currentSolutionCost = tempCurrent.computeCost();
			double extendedSolutionCost = tempExtended.computeCost();
			double tempMarginal = Math.max(0.0, extendedSolutionCost - currentSolutionCost);
//			System.out.println("TEMP MARGINAL = " + tempMarginal + "  ::  MARGINAL = " + worstMarginalCost);
			if (tempMarginal < worstMarginalCost && tempMarginal != 0.0) {
				worstMarginalCost = tempMarginal;
			}
		}
//		return Math.max(0, extendedSolutionCost - currentSolutionCost);

		return worstMarginalCost;
	}

	// Simple computation of marginal cost - difference between extended solution
	// and current solution
	public double computeSimpleMarginalCost(Task taskToAdd) {
		double marginalCost = 0.0;

		// TODO: do stuff
//		this.currentSolution = this.getBestSlsSolution(agent.vehicles(), this.tasksSoFar, this.iterationsBound, this.p, this.startTime, this.pickRandom);
		ArrayList<Task> extendedTasks = new ArrayList<Task>(this.taskArray);
		extendedTasks.add(taskToAdd);
		this.extendedSolution = this.getBestSlsSolution(agent.vehicles(), extendedTasks, this.iterationsBound, this.p,
				this.startTime, this.pickRandom);

		double currentSolutionCost = this.currentSolution.computeCost();
		double extendedSolutionCost = this.extendedSolution.computeCost();

		return Math.max(0, extendedSolutionCost - currentSolutionCost);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

		// System.out.println("Agent " + agent.id() + " has tasks " + tasks);

//		System.out.println("Start planning: " + this.taskArray);
		long time_start = System.currentTimeMillis();

		List<Plan> plans = slsPlans(vehicles, this.taskArray, iterationsBound, p, time_start, pickRandom);
//		System.out.println("Done planning");
		double cost = 0.0;
		for (int i = 0; i < plans.size(); i++) {
			cost += plans.get(i).totalDistance() * vehicles.get(i).costPerKm();
		}
		System.out.println("TOTAL SMART COST = " + cost + " TOTAL SMART PROFIT = " + (this.totalBidsWon - cost));
		System.out.println("BIDDING HISTORY:");
		for (int i = 0; i < this.agentsBidsHistory.size(); i++) {
			if (i == agent.id()) {
				System.out.println("Our agent bids are: " + this.agentsBidsHistory.get(i));
			} else {
				System.out.println("Other agent bids are: " + this.agentsBidsHistory.get(i));
			}
		}
		long time_end = System.currentTimeMillis();
//		long duration = time_end - time_start;

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
	private Solution getBestSlsSolution(List<Vehicle> vehicles, ArrayList<Task> tasks, int iterationsBound, double p,
			long startTime, boolean pickRandom) {
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
		return bestSoFar;
	}

	private List<Plan> buildAgentPlansFromSolution(List<Vehicle> vehicles, Solution s) {
		List<Plan> optimalVehiclePlans = new ArrayList<Plan>(vehicles.size());
		for (Vehicle v : vehicles) {
			LinkedList<DecentralizedAction> actions = s.getActions().get(v);
			Plan plan = s.buildPlanFromActionList(actions, v.getCurrentCity());
			optimalVehiclePlans.add(plan);
		}
		return optimalVehiclePlans;
	}

	private List<Plan> slsPlans(List<Vehicle> vehicles, ArrayList<Task> tasks, int iterationsBound, double p,
			long startTime, boolean pickRandom) {

		Solution bestSolution = getBestSlsSolution(vehicles, tasks, iterationsBound, p, startTime, pickRandom);
		System.out.println("# SMART TASKS with future = " + bestSolution.getTasks().size());
		// Build logist plan for every vehicle from the actions in the best solution
		// found
		return this.buildAgentPlansFromSolution(vehicles, bestSolution);
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

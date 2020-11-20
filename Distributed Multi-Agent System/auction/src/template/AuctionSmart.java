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
	private HashMap<Integer, Integer> winCounts;
	private long totalBidsWon = 0;

	private long setupTimeout, planTimeout, bidTimeout;

	private double increaseRate = 0.5;
	private double risk = 0.9;
	int iterationsBound = 10000;
	double p = 0.3;
	//long startTime;
	boolean pickRandom = true;
	int slsPredictionsSize = 10;
	int predictedTasksCount = 5;
	boolean minBidHigher = true;
	
	//TESTING
	long bidStart = 0;

	@Override
	public void setup(Topology topology, TaskDistribution distribution, Agent agent) {

		this.agentsBidsHistory = new HashMap<Integer, List<Long>>();
		this.winCounts = new HashMap<Integer, Integer>();
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
			System.out.println("We won and the minBid of the other agent was higher than our marginal cost: " + this.minBidHigher);
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
			this.winCounts.putIfAbsent(i, 0);
			this.winCounts.put(winnerId, this.winCounts.getOrDefault(winnerId, 0) + 1);

		}
		//long bidEnd = System.currentTimeMillis();
		//System.out.println("Bidding took: " + (bidEnd - bidStart));
	}

	@Override
	public Long askPrice(Task task) {

		bidStart = System.currentTimeMillis();
		Vehicle biggestVehicle = Utils.findBiggestVehicle(this.agent.vehicles());
		if (biggestVehicle.capacity() < task.weight) {
			return null;
		}

		long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity);
		long distanceSum = distanceTask + currentCity.distanceUnitsTo(task.pickupCity);

		double marginalCost = computeLessSimpleMarginalCost(task);

		// double ratio = 1.0 + (random.nextDouble() * this.increaseRate * task.id);
		double bid = computeBid(marginalCost);
		//System.out.println("MARGINAL = " + marginalCost + "  ::  BID = " + bid);

		return (long) Math.round(bid);
	}

	// Simple computation of marginal cost - difference between extended solution
	// and current solution
	public double computeLessSimpleMarginalCost(Task taskToAdd) {
		double marginalCost = 0.0;

		long startTimeMarginalCost = System.currentTimeMillis();
		long noFutureTimeOut = 2*(bidTimeout/5);
//		this.currentSolution = this.getBestSlsSolution(agent.vehicles(), this.tasksSoFar, this.iterationsBound, this.p, this.startTime, this.pickRandom);
		ArrayList<Task> extendedTasks = new ArrayList<Task>(this.taskArray);
		extendedTasks.add(taskToAdd);
		this.extendedSolution = this.getBestSlsSolution(agent.vehicles(), extendedTasks, this.iterationsBound, this.p,
				noFutureTimeOut, this.pickRandom);

		double currentSolutionCost = this.currentSolution.computeCost();
		double extendedSolutionCost = this.extendedSolution.computeCost();

		marginalCost = Math.max(0, extendedSolutionCost - currentSolutionCost);
//

		long endTimeMarginalCost = System.currentTimeMillis();
		long timeLeft = bidTimeout - (endTimeMarginalCost - startTimeMarginalCost);
		
		long futureTimeOut = (long) ((90*timeLeft/100)/slsPredictionsSize)/2;
		//System.out.println("Time left is: " + timeLeft);
		//System.out.println("Future time out is: " + futureTimeOut);
		// ADD future prediction task
		double worstMarginalCost = Double.MAX_VALUE;
		
		for (int i = 0; i < slsPredictionsSize; i++) {
			ArrayList<Task> futureTasks = new ArrayList<Task>(this.taskArray);
			ArrayList<Task> futureExtendedTasks = new ArrayList<Task>(futureTasks);
			futureExtendedTasks.add(taskToAdd);

			Task predictedTask = null;
			for (int j = 0; j < this.predictedTasksCount; j++) {
				predictedTask = this.defaultDistribution.createTask();
				futureTasks.add(predictedTask);
				futureExtendedTasks.add(predictedTask);
			}
			
			Solution tempCurrent = this.getBestSlsSolution(agent.vehicles(), futureTasks, this.iterationsBound, this.p,
					futureTimeOut, this.pickRandom);
			Solution tempExtended = this.getBestSlsSolution(agent.vehicles(), futureExtendedTasks, this.iterationsBound,
					this.p, futureTimeOut, this.pickRandom);

			currentSolutionCost = tempCurrent.computeCost();
			extendedSolutionCost = tempExtended.computeCost();
			double tempMarginal = Math.max(0.0, extendedSolutionCost - currentSolutionCost);
//			System.out.println("TEMP MARGINAL = " + tempMarginal + "  ::  MARGINAL = " + worstMarginalCost);
			if (tempMarginal < worstMarginalCost && tempMarginal != 0.0) {
				worstMarginalCost = tempMarginal;
			}
		}
//		return Math.max(0, extendedSolutionCost - currentSolutionCost);

		//return worstMarginalCost;
		double bid = Math.max(marginalCost, worstMarginalCost);
		//bid = marginalCost - (marginalCost - bid) * 0.1;
		return bid;
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
				this.bidTimeout, this.pickRandom);

		double currentSolutionCost = this.currentSolution.computeCost();
		double extendedSolutionCost = this.extendedSolution.computeCost();

		return Math.max(0, extendedSolutionCost - currentSolutionCost);
	}

	@Override
	public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {

		// System.out.println("Agent " + agent.id() + " has tasks " + tasks);

//		System.out.println("Start planning: " + this.taskArray);
		//long time_start = System.currentTimeMillis();

		List<Plan> plans = slsPlans(vehicles, this.taskArray, iterationsBound, p, planTimeout - 500, pickRandom);
//		System.out.println("Done planning");
		double cost = 0.0;
		for (int i = 0; i < plans.size(); i++) {
			cost += plans.get(i).totalDistance() * vehicles.get(i).costPerKm();
		}
		//System.out.println("TOTAL SMART COST = " + cost + " TOTAL SMART PROFIT = " + (this.totalBidsWon - cost));
		/*System.out.println("BIDDING HISTORY:");
		for (int i = 0; i < this.agentsBidsHistory.size(); i++) {
			if (i == agent.id()) {
				System.out.println("Our agent bids are: " + this.agentsBidsHistory.get(i));
			} else {
				System.out.println("Other agent bids are: " + this.agentsBidsHistory.get(i));
			}
		}
		long time_end = System.currentTimeMillis();*/
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
			long timeOut, boolean pickRandom) {
		
		long startTime = System.currentTimeMillis();
		
		Solution currentBestSolution = new Solution(vehicles, tasks);
		currentBestSolution.createRandomInitialSolution();

		Solution bestSoFar = currentBestSolution;

		int iterationCount = 0;
		// iterate until we reach iterationsBound or we timeout
		while (iterationCount < iterationsBound && timeOut(startTime, timeOut)) {
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
			long timeOut, boolean pickRandom) {

		Solution bestSolution = getBestSlsSolution(vehicles, tasks, iterationsBound, p, timeOut, pickRandom);
		//System.out.println("# SMART TASKS with future = " + bestSolution.getTasks().size());
		// Build logist plan for every vehicle from the actions in the best solution
		// found
		return this.buildAgentPlansFromSolution(vehicles, bestSolution);
	}

	private boolean timeOut(long startTime, long timeout) {
		long currentTime = System.currentTimeMillis();
		long duration = currentTime - startTime;
		// Half of a second to build a plan
		return duration < (90 * timeout / 100);
	}

	public long computeBid(double marginalCost) {
		double bid = 0;
		//We skip the first round because there is no available info of the other agent
		if (this.winCounts.size() == 0) {
			return (long) marginalCost;
		}

		int idx = getBestOpponentAgentIndex();
		
		//Get the minimal bet of the other best agent
		Long minBid = getOtherAgentMinBid(agentsBidsHistory.get(idx));

		//Add 30% of our marginal cost to their minBid if we bid too low
		if (minBid > marginalCost) {
			this.minBidHigher = true;
			bid = 0.3 * marginalCost + minBid;
		} else {
			this.minBidHigher = false;
			bid = marginalCost;
		}

		return (long) Math.ceil(bid);
	}

	//This entire function is useless if we play only against one agent
	public int getBestOpponentAgentIndex() {
		int bestAgentId = 0;
		int winNumberBestAgent = -1;

		for (int id : agentsBidsHistory.keySet()) {
			if (id == agent.id()) {
				continue;
			}
			int bestAgenWinCount = this.winCounts.get(id);

			//Useless if we are playing against one other agent
			if (bestAgenWinCount > winNumberBestAgent) {
				winNumberBestAgent = bestAgenWinCount;
				bestAgentId = id;
			}
		}

		return bestAgentId;
	}

	//Look at 5 last bids the best agent has made and return the smallest one
	public Long getOtherAgentMinBid(List<Long> agentBidHistory) {
		double minimalBid = Double.MAX_VALUE;

		//Check the 5 newest bids of the other agent
		for (int i = agentBidHistory.size() - 1; i >= (agentBidHistory.size() - 1 - Math.min(5, agentBidHistory.size() - 1)); i--) {
			if(agentBidHistory.get(i) < minimalBid) {
				minimalBid = agentBidHistory.get(i);
			}
		}

		return (long) minimalBid;
	}
}

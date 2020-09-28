import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.util.SimUtilities;
import uchicago.src.sim.analysis.DataSource;
import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Sequence;

import java.awt.Color;
import java.util.ArrayList;

/**
 * Class that implements the simulation model for the rabbits grass
 * simulation.  This is the first class which needs to be setup in
 * order to run Repast simulation. It manages the entire RePast
 * environment and the simulation.
 *
 * @author 
 */


public class RabbitsGrassSimulationModel extends SimModelImpl {		
	
		// Default values for GUI variables
		private static final int DEFAULTGRIDSIZE = 20;
		private static final int DEFAULTINITRABBITS = 10;
		private static final int DEFAULTINITGRASS = 10;
		private static final int DEFAULTGRASSGROWTHRATE = 5;
		private static final int DEFAULTBIRTHTHRESHOLD = 10;
		private static final int DEFAULTMAXINITRABBITENERGY = 10;
		private static final int DEFAULTMAXGRASSENERGY = 15;
		private static final int DEFAULTMAXINITIALGRASSENERGY = 5;
		
		// Initialize variables to their default values
		private int gridSize = DEFAULTGRIDSIZE;
		private int numInitRabbits = DEFAULTINITRABBITS;
		private int numInitGrass = DEFAULTINITGRASS;
		private int grassGrowthRate = DEFAULTGRASSGROWTHRATE;
		private int birthThreshold = DEFAULTBIRTHTHRESHOLD;
		private int maxInitialRabbitEnergy = DEFAULTMAXINITRABBITENERGY;
		private int maxGrassEnergy = DEFAULTMAXGRASSENERGY;
		private int maxInitialGrassEnergy = DEFAULTMAXINITIALGRASSENERGY;
		
		private String simulationName = "Rabbits Grass Simulation";
		
		private Schedule schedule;
		
		private RabbitsGrassSimulationSpace rabbitsGrassSpace;
		private DisplaySurface displaySurface;
		private ArrayList<RabbitsGrassSimulationAgent> rabbits;
		
		private OpenSequenceGraph totalGrassAndRabbitEnergies;
		private OpenSequenceGraph grassPatchesAndRabbitsCounts;
		
		// Collect data about total grass energy available
		class grassEnergyAvailable implements DataSource, Sequence {
			
			public Object execute() {
				return new Double(getSValue());
			}
			
			public double getSValue() {
				return (double)rabbitsGrassSpace.getTotalGrassEnergy();
			}
		}
		
		// Collect data about total rabbit energy present
		class rabbitEnergyPresent implements DataSource, Sequence {
			
			public Object execute() {
				return new Double(getSValue());
			}
			
			public double getSValue() {
				return (double)getRabbitEnergy();
			}

		}
		
		// Collect data about number of grass patches available
		class grassPatchesAvailable implements DataSource, Sequence {
			
			public Object execute() {
				return new Double(getSValue());
			}
			
			public double getSValue() {
				return (double)rabbitsGrassSpace.getGrassPatchesCount();
			}
		}
		
		// Collect data about number of rabbits present
		class rabbitsPresent implements DataSource, Sequence {
			
			public Object execute() {
				return new Double(getSValue());
			}
			
			public double getSValue() {
				return (double)rabbits.size();
			}
		}
		
		// Main function to kick off the simulation
		public static void main(String[] args) {
			
			System.out.println("Rabbit skeleton");

			SimInit init = new SimInit();
			RabbitsGrassSimulationModel model = new RabbitsGrassSimulationModel();
			// Do "not" modify the following lines of parsing arguments
			if (args.length == 0) // by default, you don't use parameter file nor batch mode 
				init.loadModel(model, "", false);
			else
				init.loadModel(model, args[0], Boolean.parseBoolean(args[1]));
			
		}

		// Calls functions to build the model, schedule, and display
		public void begin() {
			this.buildModel();
			this.buildSchedule();
			this.buildDisplay();
			
			this.displaySurface.display();
			this.totalGrassAndRabbitEnergies.display();
			this.grassPatchesAndRabbitsCounts.display();
		}

		public void setup() {
			// setup simulation space
			this.rabbitsGrassSpace = null;
			this.rabbits = new ArrayList<RabbitsGrassSimulationAgent>();
			this.schedule = new Schedule(1);
			
			// Nullify current display if not null
		    if (displaySurface != null){
		    	displaySurface.dispose();
		    }
		    displaySurface = null;
		    
			// Nullify current OpenSequenceGraph
			if (this.totalGrassAndRabbitEnergies != null) {
				this.totalGrassAndRabbitEnergies.dispose();
			}
			this.totalGrassAndRabbitEnergies = null;
			
			if (this.grassPatchesAndRabbitsCounts != null) {
				this.grassPatchesAndRabbitsCounts.dispose();
			}
			this.grassPatchesAndRabbitsCounts = null;
			
		    // Reinitialize display and sequence graph
		    displaySurface = new DisplaySurface(this, "Rabbits Grass Simulation Window");
		    this.totalGrassAndRabbitEnergies = new OpenSequenceGraph("Total Grass And Rabbit Energies", this);
		    this.grassPatchesAndRabbitsCounts = new OpenSequenceGraph("Grass Patches And Rabbits Counts", this);
		    
		    registerDisplaySurface("Rabbits Grass Simulation Window", displaySurface);
		    this.registerMediaProducer("Plot", this.totalGrassAndRabbitEnergies);
		    this.registerMediaProducer("Plot", this.grassPatchesAndRabbitsCounts);
		}
		
		/*
		 * Builds simulation model - generates initial grass patches, initializes initial rabbits
		 */
		public void buildModel() {
			this.rabbitsGrassSpace = new RabbitsGrassSimulationSpace(this.gridSize);

			// Generate initial grass patches
			this.rabbitsGrassSpace.generateGrass(this.numInitGrass, this.maxGrassEnergy);

			// Generate initial rabbit agents
			for (int i = 0; i < this.numInitRabbits; i++) {
				addNewRabbit();
			}
			
			// Agents report
			for (int i = 0; i < this.rabbits.size(); i++) {
				RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent)this.rabbits.get(i);
				rabbit.report();
			}
		}
		
		// Creates a new rabbit agents with initial energy and adds it to the list of agents and simulation space
		private void addNewRabbit() {
		    RabbitsGrassSimulationAgent rabbit = new RabbitsGrassSimulationAgent(this.maxInitialRabbitEnergy);
		    this.rabbits.add(rabbit);
		    rabbitsGrassSpace.addAgent(rabbit);
		}
		
		/*
		 * Builds display - initializes color map and 2D agent & grass displays 
		 */
		public void buildDisplay() {
			ColorMap map = new ColorMap();
			map.mapColor(0, Color.black);
			
			for(int i = 1; i <= this.maxGrassEnergy; i++) {
				map.mapColor(i, 0, i * (1.0 / this.maxGrassEnergy), 0);
			}
			
			Value2DDisplay displayGrass = new Value2DDisplay(rabbitsGrassSpace.getCurrentGrassSpace(), map);
			displaySurface.addDisplayable(displayGrass, "Rabbits Grass");

			Object2DDisplay displayAgents = new Object2DDisplay(rabbitsGrassSpace.getCurrentRabbitsSpace());
			displayAgents.setObjectList(rabbits);

			displaySurface.addDisplayableProbeable(displayGrass, "Grass");
			displaySurface.addDisplayableProbeable(displayAgents, "Agents");
		
			this.totalGrassAndRabbitEnergies.addSequence("Grass Energy", new grassEnergyAvailable());
			this.totalGrassAndRabbitEnergies.addSequence("Rabbit Energy", new rabbitEnergyPresent());
			
			this.grassPatchesAndRabbitsCounts.addSequence("Grass Patches Count", new grassPatchesAvailable());
			this.grassPatchesAndRabbitsCounts.addSequence("Rabbits Count", new rabbitsPresent());
		}
		
		/*
		 * Builds simulation schedule
		 */
		public void buildSchedule() {
			class RabbitsGrassStep extends BasicAction {
				public void execute() {
					// Ensure that agents move in random order
					SimUtilities.shuffle(rabbits);
					int rabbitsCount = rabbits.size();
					int newBornRabbits = 0;
					
					for (int i = 0; i < rabbitsCount ; i++) {
						// Perform single step for every rabbit agent
						RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent)rabbits.get(i);
						rabbit.step();
						
						// If the energy is sufficient, a rabbit is born at a random location
						if (rabbit.getEnergy() > birthThreshold){
							addNewRabbit();
							newBornRabbits++;
							// reduce current rabbit's energy in half
							rabbit.setEnergy((int)Math.floor(rabbit.getEnergy() / 2));
						}
					}
					
					// Generate grass at given growth rate
					rabbitsGrassSpace.generateGrass(grassGrowthRate, maxGrassEnergy);
					
					// Eliminate agents with low energy (e.g. energy < 1)
					int deadRabbits = removeDeadRabbits();
					
					displaySurface.updateDisplay();
				}
			}
			
			schedule.scheduleActionBeginning(0, new RabbitsGrassStep());
			
			class RabbitsGrassUpdateGrassAndRabbitEnergies extends BasicAction {
				public void execute(){
					totalGrassAndRabbitEnergies.step();
			    }
			}
			schedule.scheduleActionAtInterval(10, new RabbitsGrassUpdateGrassAndRabbitEnergies());
			
			class RabbitsGrassUpdateGrassAndRabbitsCounts extends BasicAction {
				public void execute(){
					grassPatchesAndRabbitsCounts.step();
			    }
			}
			schedule.scheduleActionAtInterval(10, new RabbitsGrassUpdateGrassAndRabbitsCounts());
		}
		
		/*
		 * Remove rabbit agents with low energy (e.g. energy < 1)
		 */
		private int removeDeadRabbits() {
			int count = 0;
			for (int i = rabbits.size() - 1; i >= 0; i--) {
				RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent)rabbits.get(i);
				if (rabbit.getEnergy() < 1) {
					rabbitsGrassSpace.removeAgentAt(rabbit.getX(), rabbit.getY());
					rabbits.remove(i);
					count++;
				}
			}
			
			// Return number of dead rabbits
			return count;
		}
		
		public String[] getInitParam() {
			// Parameters to be set by users via the Repast UI slider bar
			// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
			String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "MaxInitialRabbitEnergy", "MaxGrassEnergy", "MaxInitialGrassEnergy"};
			return params;
		}
		
		/*
		 * Getters and Setters section
		 */
		public String getName() {
			return this.simulationName;
		}

		public Schedule getSchedule() {
			return this.schedule;
		}

		public int getGridSize() {
			return gridSize;
		}

		public void setGridSize(int gridSize) {
			this.gridSize = gridSize;
		}

		public int getNumInitRabbits() {
			return numInitRabbits;
		}

		public void setNumInitRabbits(int numInitRabbits) {
			this.numInitRabbits = numInitRabbits;
		}

		public int getNumInitGrass() {
			return numInitGrass;
		}

		public void setNumInitGrass(int numInitGrass) {
			this.numInitGrass = numInitGrass;
		}

		public int getGrassGrowthRate() {
			return grassGrowthRate;
		}

		public void setGrassGrowthRate(int grassGrowthRate) {
			this.grassGrowthRate = grassGrowthRate;
		}

		public int getBirthThreshold() {
			return birthThreshold;
		}

		public void setBirthThreshold(int birthThreshold) {
			this.birthThreshold = birthThreshold;
		}
		
		public int getMaxInitialRabbitEnergy() {
			return this.maxInitialRabbitEnergy;
		}
		
		public void setMaxInitialRabbitEnergy(int energy) {
			this.maxInitialRabbitEnergy = energy;
		}
		
		public int getMaxGrassEnergy() {
			return this.maxGrassEnergy;
		}
		
		public void setMaxGrassEnergy(int maxEnergy) {
			this.maxGrassEnergy = maxEnergy;
		}
		
		public int getMaxInitialGrassEnergy() {
			return this.maxInitialGrassEnergy;
		}
		
		public void setMaxInitialGrassEnergy(int maxEnergy) {
			this.maxInitialGrassEnergy = maxEnergy;
		}
		
		private int getRabbitEnergy() {
			int totalEnergy = 0;
			for (int i = 0; i < rabbits.size(); i ++) {
				totalEnergy += rabbits.get(i).getEnergy();
			}
			
			return totalEnergy;
		}
}

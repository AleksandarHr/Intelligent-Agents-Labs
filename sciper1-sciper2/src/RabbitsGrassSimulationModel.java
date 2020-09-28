import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.Value2DDisplay;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.util.SimUtilities;

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
	
		private static final int DEFAULTGRIDSIZE = 20;
		private static final int DEFAULTINITRABBITS = 10;
		private static final int DEFAULTINITGRASS = 10;
		private static final int DEFAULTGRASSGROWTHRATE = 5;
		private static final int DEFAULTBIRTHTHRESHOLD = 10;
		private static final int DEFAULTINITENERGY = 10;
		private static final int DEFAULTMAXGRASSENERGY = 15;
		private static final int DEFAULTMAXINITIALGRASSENERGY = 5;
		
		private int gridSize = DEFAULTGRIDSIZE;
		private int numInitRabbits = DEFAULTINITRABBITS;
		private int numInitGrass = DEFAULTINITGRASS;
		private int grassGrowthRate = DEFAULTGRASSGROWTHRATE;
		private int birthThreshold = DEFAULTBIRTHTHRESHOLD;
		private int initialEnergy = DEFAULTINITENERGY;
		private int maxGrassEnergy = DEFAULTMAXGRASSENERGY;
		private int maxInitialGrassEnergy = DEFAULTMAXINITIALGRASSENERGY;
		
		private Schedule schedule;
		
		private RabbitsGrassSimulationSpace rabbitsGrassSpace;
		private DisplaySurface displaySurface;
		private ArrayList<RabbitsGrassSimulationAgent> rabbits;
		
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

		public void begin() {
			this.buildModel();
			this.buildSchedule();
			this.buildDisplay();
			
			displaySurface.display();
		}

		public void setup() {
			// setup simulation space
			this.rabbitsGrassSpace = null;
			this.rabbits = new ArrayList<RabbitsGrassSimulationAgent>();
			this.schedule = new Schedule(1);
			
			// remove display
		    if (displaySurface != null){
		    	displaySurface.dispose();
		    }
		    displaySurface = null;
		    
		    // reinitialize display
		    displaySurface = new DisplaySurface(this, "Rabbits Grass Simulation Window");
		    registerDisplaySurface("Rabbits Grass Simulation Window", displaySurface);
		}
		
		public void buildModel() {
			this.rabbitsGrassSpace = new RabbitsGrassSimulationSpace(this.gridSize);
			this.rabbitsGrassSpace.generateGrass(this.numInitGrass, this.maxGrassEnergy);

			for (int i = 0; i < this.numInitRabbits; i++) {
				addNewRabbit();
			}
			
			for (int i = 0; i < this.rabbits.size(); i++) {
				RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent)this.rabbits.get(i);
				rabbit.report();
			}
		}
		
		private void addNewRabbit() {
		    RabbitsGrassSimulationAgent rabbit = new RabbitsGrassSimulationAgent(this.initialEnergy);
		    this.rabbits.add(rabbit);
		    rabbitsGrassSpace.addAgent(rabbit);
		}
		
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
		}
		
		public void buildSchedule() {
			class RabbitsGrassStep extends BasicAction {
				public void execute() {
					SimUtilities.shuffle(rabbits);
					int rabbitsCount = rabbits.size();
					int newBornRabbits = 0;
					
					for (int i = 0; i < rabbitsCount ; i++) {
						RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent)rabbits.get(i);
						rabbit.step();
						
						//A rabbit is born at random location if the energy is sufficient
						if( rabbit.getEnergy() > birthThreshold){
							addNewRabbit();
							newBornRabbits++;
						}
					}
					
					//Grow grass
					rabbitsGrassSpace.generateGrass(grassGrowthRate, maxGrassEnergy);
					int deadRabbits = removeDeadRabbits();
					
					displaySurface.updateDisplay();
				}
			}
			
			schedule.scheduleActionBeginning(0, new RabbitsGrassStep());
		}
		
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
			
			return count;
		}
		
		public String[] getInitParam() {
			// TODO Auto-generated method stub
			// Parameters to be set by users via the Repast UI slider bar
			// Do "not" modify the parameters names provided in the skeleton code, you can add more if you want 
			String[] params = { "GridSize", "NumInitRabbits", "NumInitGrass", "GrassGrowthRate", "BirthThreshold", "InitialEnergy", "MaxGrassEnergy", "MaxInitialGrassEnergy"};
			return params;
		}

		public String getName() {
			// TODO Auto-generated method stub
			return null;
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
		
		public int getInitialEnergy() {
			return this.initialEnergy;
		}
		
		public void setInitialEnergy(int energy) {
			this.initialEnergy = energy;
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
}

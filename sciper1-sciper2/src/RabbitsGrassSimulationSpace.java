/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */
import uchicago.src.sim.space.Object2DGrid;
import uchicago.src.sim.util.Random;

public class RabbitsGrassSimulationSpace {
	
	// 2D Grid spaces to keep track of the grass and the rabbits
	private Object2DGrid grassSpace;
	private Object2DGrid rabbitsSpace;
	
	// Constructor for the simulation space object
	public RabbitsGrassSimulationSpace (int size) {
		// Initialize the grass and rabits grid objects
		this.grassSpace = new Object2DGrid(size, size);
		this.rabbitsSpace = new Object2DGrid(size, size);
		
		// Initialize the grass grid celss
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				grassSpace.putObjectAt(x, y, new Integer(0));
			}
		}
	}
	
	/*
	 * Generate grassCount number of new grass patches, each of at most maxGrassEnergy energy
	 */
	public void generateGrass(int grassCount, int maxGrassEnergy) {
		if (grassCount > 0 && maxGrassEnergy > 0) {
			for (int i = 0; i < grassCount; i++) {
				// Choose random coordinates to place the grass
				Random.createUniform();
				int grassX = (int)(Math.random()*(grassSpace.getSizeX()));
				int grassY = (int)(Math.random()*(grassSpace.getSizeY()));
				// Choose random amount of energy in the range [1 ; MaxGrassEnergy]
				int newGrassEnergy = (int)(Math.random()*maxGrassEnergy) + 1;
		      
				// If there is already grass present at (grassX, grassY), accumulate the energy of old and new grass
				if (this.isGrassSpaceCellOccupied(grassX, grassY)) {
					int currentGrass = (int) grassSpace.getValueAt(grassX, grassY);
					newGrassEnergy = (newGrassEnergy + currentGrass) % maxGrassEnergy;
				}
				grassSpace.putObjectAt(grassX, grassY, new Integer(newGrassEnergy));
			}
		}	
	}
	
	// Checks if an agent is present at given coordinates
	private boolean isAgentSpaceCellOccupied (int x, int y) {
		return rabbitsSpace.getObjectAt(x, y) != null;
	}
	
	// Checks if a grass patch is present at given coordinates
	private boolean isGrassSpaceCellOccupied (int x, int y) {
		return grassSpace.getObjectAt(x, y) != null;
	}
	
	/*
	 * Generates a new agents and positions it at a random cell on the grid
	 */
	public boolean addAgent(RabbitsGrassSimulationAgent agent) {
		boolean added = false;
		Random.createUniform();
		// Since we create a Uniform distribution, trialLimit is set to the number of possible cells
		int trialLimit = this.rabbitsSpace.getSizeX() * this.rabbitsSpace.getSizeY();
		int trialCount = 0;

		// Generate random coordinates until agent is successfully placed (e.g. empty cell is chosen)
		//		or trial limit is reached
		while (!added && trialCount < trialLimit) {
			int x = (int)(Math.random()*(rabbitsSpace.getSizeX()));
		    int y = (int)(Math.random()*(rabbitsSpace.getSizeY()));
		    if(isAgentSpaceCellOccupied(x,y) == false){
		    	rabbitsSpace.putObjectAt(x,y,agent);
		        agent.setCoordinates(x,y);
		        agent.setRabbitsGrassSpace(this);
		        added = true;
		    }
		}
		return added;
	}

	/*
	 * Removes agent at given coordinates
	 */
	public void removeAgentAt(int x, int y) {
		this.rabbitsSpace.putObjectAt(x, y, null);
	}
	
	/*
	 * Removes grass from given cell and returns the energy it contains
	 */
	public int eatGrassAt(int x, int y) {
		if (this.grassSpace.getObjectAt(x, y) != null) {
			int grass = (Integer) this.grassSpace.getObjectAt(x, y);
			this.grassSpace.putObjectAt(x, y, new Integer(0));
			return grass;
		}
		return 0;
	}
	
	/*
	 * Tries to perform a rabbit movement 
	 * Returns true if successful and false otherwise
	 */
	public boolean moveRabbitTo(int x, int y, int newX, int newY) {
		boolean moveSuccessful = false;
		
		// Only move the rabbit to the new cell if it is not already occupied by another rabbit
		if (!this.isAgentSpaceCellOccupied(newX, newY)) {
			RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent)this.rabbitsSpace.getObjectAt(x, y);
			this.removeAgentAt(x, y);
			rabbit.setCoordinates(newX, newY);
			this.rabbitsSpace.putObjectAt(newX, newY, rabbit);
			moveSuccessful = true;
		}
		
		return moveSuccessful;
	}
	
	// Getters and Setters section
	public Object2DGrid getCurrentGrassSpace() {
		return this.grassSpace;
	}
	
	public Object2DGrid getCurrentRabbitsSpace() {
		return this.rabbitsSpace;
	}
	
	/*
	 * Returns the total sum of grass energy available
	 */
	public int getTotalGrassEnergy() {
		int totalEnergy = 0;
		for (int x = 0; x < this.grassSpace.getSizeX(); x++) {
			for (int y = 0; y < this.grassSpace.getSizeY(); y++) {
				if (this.grassSpace.getObjectAt(x, y) != null) {
					totalEnergy += ((Integer)this.grassSpace.getObjectAt(x, y)).intValue();
				}
			}
		}
		return totalEnergy;
	}
	
}

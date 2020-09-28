/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */
import uchicago.src.sim.space.Object2DGrid;

public class RabbitsGrassSimulationSpace {
	private Object2DGrid grassSpace;
	private Object2DGrid rabbitsSpace;
	
	public RabbitsGrassSimulationSpace (int size) {
		this.grassSpace = new Object2DGrid(size, size);
		this.rabbitsSpace = new Object2DGrid(size, size);
		
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				grassSpace.putObjectAt(x, y, new Integer(0));
			}
		}
	}
	
	public void generateGrass(int numInitGrass, int maxGrassEnergy) {
		for (int i = 0; i < numInitGrass; i++) {
			int grassX = (int)(Math.random()*(grassSpace.getSizeX()));
		    int grassY = (int)(Math.random()*(grassSpace.getSizeY()));
		    int grassEnergy = (int)(Math.random()*maxGrassEnergy) + 1;
		      
		    // Try to place grass patch on a random coordinate
		    if (this.isGrassSpaceCellOccupied(grassX, grassY)) {
		    	int currentGrass = (int) grassSpace.getValueAt(grassX, grassY);
		    	grassEnergy += currentGrass;
		    }
		    grassSpace.putObjectAt(grassX, grassY, new Integer(grassEnergy));
		}
	}
	
	private boolean isAgentSpaceCellOccupied (int x, int y) {
		return rabbitsSpace.getObjectAt(x, y) != null;
	}
	
	private boolean isGrassSpaceCellOccupied (int x, int y) {
		return grassSpace.getObjectAt(x, y) != null;
	}
	
	public boolean addAgent(RabbitsGrassSimulationAgent agent) {
		boolean added = false;
		int trialLimit = this.rabbitsSpace.getSizeX() * this.rabbitsSpace.getSizeY();
		int trialCount = 0;
		
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

	public void removeAgentAt(int x, int y) {
		this.rabbitsSpace.putObjectAt(x, y, null);
	}
	
	public int eatGrassAt(int x, int y) {
		if (this.grassSpace.getObjectAt(x, y) != null) {
			int grass = (Integer) this.grassSpace.getObjectAt(x, y);
			this.grassSpace.putObjectAt(x, y, new Integer(0));
			//return grass;
			return grass;
		}
		return 0;
	}
	
	/*
	 * Tries to perform a rabbit movement, returns true if successfull and false otherwise
	 */
	public boolean moveRabbitTo(int x, int y, int newX, int newY) {
		boolean moveSuccessful = false;
		
		if (!this.isAgentSpaceCellOccupied(newX, newY)) {
			RabbitsGrassSimulationAgent rabbit = (RabbitsGrassSimulationAgent)this.rabbitsSpace.getObjectAt(x, y);
			this.removeAgentAt(x, y);
			rabbit.setCoordinates(newX, newY);
			this.rabbitsSpace.putObjectAt(newX, newY, rabbit);
			moveSuccessful = true;
		}
		
		return moveSuccessful;
	}
	
	// Getters
	public Object2DGrid getCurrentGrassSpace() {
		return this.grassSpace;
	}
	
	public Object2DGrid getCurrentRabbitsSpace() {
		return this.rabbitsSpace;
	}
	
}

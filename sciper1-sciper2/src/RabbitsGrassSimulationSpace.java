/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */
import uchicago.src.sim.space.Object2DTorus;

public class RabbitsGrassSimulationSpace {
	private Object2DTorus grassSpace;
	private Object2DTorus rabbitsSpace;
	
	public RabbitsGrassSimulationSpace (int size) {
		this.grassSpace = new Object2DTorus(size, size);
		this.rabbitsSpace = new Object2DTorus(size, size);
	}
	
	public void generateRabbits(int numInitRabbits) {

	}
	
	public void generateGrass(int numInitGrass) {
		for (int i = 0; i < numInitGrass; i++) {
			int grassX = (int)(Math.random()*(grassSpace.getSizeX()));
		    int grassY = (int)(Math.random()*(grassSpace.getSizeY()));
		      
		    // Try to place grass patch on a random coordinate
		    if(this.grassSpace.getObjectAt(grassX, grassY) == null) {
		    	grassSpace.putObjectAt(grassX, grassY, new Integer(1));
		    }
		}
	}
	
	private boolean isAgentSpaceCellOccupied (int x, int y) {
		return rabbitsSpace.getObjectAt(x, y) != null;
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
		        added = true;
		    }
		}
		return added;
	}
	
	public Object2DTorus getCurrentGrassSpace() {
		return this.grassSpace;
	}
	
	public Object2DTorus getCurrentRabbitsSpace() {
		return this.rabbitsSpace;
	}

	public void removeAgentAt(int x, int y) {
		this.rabbitsSpace.putObjectAt(x, y, null);
	}
	
}

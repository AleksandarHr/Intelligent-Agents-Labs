/**
 * Class that implements the simulation space of the rabbits grass simulation.
 * @author 
 */
import uchicago.src.sim.space.Object2DTorus;

public class RabbitsGrassSimulationSpace {
	private Object2DTorus rabbitGrassSpace;
	
	public RabbitsGrassSimulationSpace (int size) {
		this.rabbitGrassSpace = new Object2DTorus(size, size);
	}
	
	public void generateRabbits(int numInitRabbits) {

	}
	
	public void generateGrass(int numInitGrass) {
		for (int i = 0; i < numInitGrass; i++) {
			int grassX = (int)(Math.random()*(rabbitGrassSpace.getSizeX()));
		    int grassY = (int)(Math.random()*(rabbitGrassSpace.getSizeY()));
		      
		    // Try to place grass patch on a random coordinate
		    if(this.rabbitGrassSpace.getObjectAt(grassX, grassY) == null) {
		    	rabbitGrassSpace.putObjectAt(grassX, grassY, new Integer(1));
		    }
		}
	}
	
	private boolean isCellOccupied (int x, int y) {
		return rabbitGrassSpace.getObjectAt(x, y) != null;
	}
	
	public Object2DTorus getCurrentSpace() {
		return this.rabbitGrassSpace;
	}
	
	
}

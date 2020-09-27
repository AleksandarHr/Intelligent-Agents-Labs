import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;


/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int energy;
	
	public RabbitsGrassSimulationAgent(int x, int y, int initialEnergy) {
		this.x = x;
		this.y = y;
		this.energy = initialEnergy;
	}
	
	public void draw(SimGraphics arg0) {
		// TODO Auto-generated method stub
		
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}
	
	public int getEnergy() {
		return this.energy;
	}
	
	public void setEnergy(int energy) {
		this.energy = energy;
	}

	public void setCoordinates(int x, int y) {
		this.x = x;
		this.y = y;
	}
}

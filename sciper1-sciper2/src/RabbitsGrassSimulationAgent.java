import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import java.awt.Color;

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
	
	public RabbitsGrassSimulationAgent(int initialEnergy) {
		this.x = -1;
		this.y = -1;
		this.energy = initialEnergy;
	}
	
	public void draw(SimGraphics G) {
		if(this.energy > 10) {
			G.drawFastRoundRect(Color.white);
		} else {
			G.drawFastRoundRect(Color.gray);
		}
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
	
	public void step() {
		this.energy--;
	}
}

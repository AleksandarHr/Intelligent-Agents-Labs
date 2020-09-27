import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import java.awt.Color;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	private int x;
	private int y;
	private int moveX;
	private int moveY;
	private int energy;
	
	private RabbitsGrassSimulationSpace rabbitsGrassSpace;
	
	public RabbitsGrassSimulationAgent(int initialEnergy) {
		this.x = -1;
		this.y = -1;
		setMoveXMoveY();
		this.energy = initialEnergy;
	}
	
	private void setMoveXMoveY() {
		this.moveX = 0;
		this.moveY = 0;
		while ((moveX == 0) && (moveY == 0)) {
			moveX = (int)Math.floor(Math.random() * 3) - 1;
			moveY = (int)Math.floor(Math.random() * 3) - 1;
		}
	}
	
	public void draw(SimGraphics G) {
		G.drawFastRoundRect(Color.white);
	}

	public void step() {
		// Move agent
		int newX = x + moveX;
		int newY = y + moveY;
		
		Object2DGrid grid = this.rabbitsGrassSpace.getCurrentRabbitsSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();
		
		if (tryMoveRabbit(newX, newY)) {
			this.energy += this.rabbitsGrassSpace.eatGrassAt(x, y);			
		} else {
			this.setMoveXMoveY();
		}
		
		this.energy--;
	}
	
	// Try to move the agent to new coordinates
	private boolean tryMoveRabbit(int newX, int newY) {
		return this.rabbitsGrassSpace.moveRabbitTo(x, y, newX, newY);
	}
	
	// Getters and Setters
	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public void setCoordinates(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public int getEnergy() {
		return this.energy;
	}
	
	public void setEnergy(int energy) {
		this.energy = energy;
	}

	public void setRabbitsGrassSpace(RabbitsGrassSimulationSpace space) {
		this.rabbitsGrassSpace = space;
	}
}

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import java.awt.Color;

import uchicago.src.sim.util.Random;
import uchicago.src.sim.space.Object2DGrid;

/**
 * Class that implements the simulation agent for the rabbits grass simulation.

 * @author
 */

public class RabbitsGrassSimulationAgent implements Drawable {

	// Class variables to keep track of agent's position, movement and energy
	private int x;
	private int y;
	private int moveX;
	private int moveY;
	private int energy;
	
	private static int IDNumber = 0;
	private int ID;
	
	private RabbitsGrassSimulationSpace rabbitsGrassSpace;
	
	// Constructor for the agent object
	public RabbitsGrassSimulationAgent(int maxInitialEnergy) {
		this.x = -1;
		this.y = -1;
		this.energy =  (int)(Math.random()*maxInitialEnergy) + 1;
		
		IDNumber++;
		this.ID = IDNumber;
	}
	
	/*
	 * Randomly choose a direction to move the agent - up/down/left/right
	 */
	private void setMoveXMoveY() {	
		this.moveX = 0;
		this.moveY = 0;
		
		Random.createUniform();
		int direction = (int)Math.floor(Math.random()*4);
		if (direction == 0) {
			// move left
			this.moveX = -1;
		} else if (direction == 1) {
			// move up
			this.moveY = -1;
		} else if (direction == 2) {
			// move right
			this.moveX = 1;
		} else if (direction == 3) {
			// move down
			this.moveY = 1;
		}
	}
	
	public void draw(SimGraphics G) {
		G.drawFastRoundRect(Color.white);
	}

	/*
	 * Step function implements the agent's behavior on every tick of the simulation
	 * 		Try to move to a random neighboring cell
	 * 		If there is any grass on the new cell eat it and increase energy
	 * 		Decrement energy due to the step made
	 */
	public void step() {
		// Move agent
		this.setMoveXMoveY();
		int newX = x + moveX;
		int newY = y + moveY;
		
		Object2DGrid grid = this.rabbitsGrassSpace.getCurrentRabbitsSpace();
		newX = (newX + grid.getSizeX()) % grid.getSizeX();
		newY = (newY + grid.getSizeY()) % grid.getSizeY();
		
		// Try to move the rabbit to the new location
		if (tryMoveRabbit(newX, newY)) {
			// If the move was successful, eat any grass that might be there
			this.energy += this.rabbitsGrassSpace.eatGrassAt(newX, newY);	
		} else {
			// If the move was not successful, eat any grass that might have been generated
			//		at the current location of the rabbit
			this.energy += this.rabbitsGrassSpace.eatGrassAt(x, y);
		}
		
		this.energy--;
	}
	
	/*
	 * Try to move the agent to (newX, newY) coordinates
	 * Return true if move was successful, false otherwise
	 */
	private boolean tryMoveRabbit(int newX, int newY) {
		return this.rabbitsGrassSpace.moveRabbitTo(x, y, newX, newY);
	}
	
	/*
	 * Reports agent's ID, current location's coordinates, and current energy
	 */
	public void report() {
		System.out.println(this.getID() + 
							" at " +
							x + ", " + y + 
							" has " + 
							this.getEnergy() + " energy left.");
	}
	
	/*
	 * Getters and Setters section
	 */
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
	
	public String getID() {
		return "Rabbit-" + ID;
	}
}

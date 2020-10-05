package template;

import logist.topology.Topology.City;
import java.util.ArrayList;
import java.util.List;

public class RoadAction {
	
	//Number of possible actions would be number of cities
	private City nextCity;
	private RoadActionType actionChosen;
	// PICKUP action contains DELIVER action in itself - as soon as the agent picks up a task,
	//		it goes on to deliver it right away
	public enum RoadActionType {
		MOVE,
		//DELIVER,
		PICKUP
	}
		
	public RoadAction(City nextCity, RoadActionType actionChosen) {
		this.nextCity = nextCity;
		this.actionChosen = actionChosen;
	}

	public boolean isDelivery() {
		return nextCity == null;
	}
	

	public void setNeighborCity(City nextCity) {
		this.nextCity = nextCity;
	}
	
	public City getNextCity() {
		return nextCity;
	}
	
	public RoadActionType getActionType() {
		return actionChosen;
	}
}

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actionChosen == null) ? 0 : actionChosen.hashCode());
		result = prime * result + ((nextCity == null) ? 0 : nextCity.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RoadAction other = (RoadAction) obj;
		if (actionChosen != other.actionChosen)
			return false;
		if (nextCity == null) {
			if (other.nextCity != null)
				return false;
		} else if (!nextCity.equals(other.nextCity))
			return false;
		return true;
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

package template;

import logist.topology.Topology.City;
import java.util.ArrayList;
import java.util.List;

public class RoadAction {
	
	//Number of possible actions would be number of cities
	private City nextCity;
	private RoadActionType actionChosen;
	public enum RoadActionType {
		MOVE,
		PICKUP
	}
	private static ArrayList<RoadAction> actions = new ArrayList<RoadAction>(); //list of all possible cities to go to
		
	public RoadAction(City nextCity, RoadActionType actionChosen) {
		this.nextCity = nextCity;
		this.actionChosen = actionChosen;
	}
	
	public static void initActions(List<City> cities) {
		for (City city : cities) {
			RoadAction.actions.add(new RoadAction(city, RoadActionType.PICKUP));
			for (City nextCity : city.neighbors()) {
				RoadAction.actions.add(new RoadAction(nextCity, RoadActionType.MOVE));
			}
		}
	}

	public boolean isDelivery() {
		return nextCity == null;
	}
	

	public void setNeighborCity(City nextCity) {
		this.nextCity = nextCity;
	}

	public static ArrayList<RoadAction> getActions() {
		return actions;
	}

	public static void setActions(ArrayList<RoadAction> actions) {
		RoadAction.actions = actions;
	}
	
	public City getNextCity() {
		return nextCity;
	}
	
	public RoadActionType getActionType() {
		return actionChosen;
	}

}

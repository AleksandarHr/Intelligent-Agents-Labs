package template;

import logist.topology.Topology.City;
import java.util.List;
import java.util.ArrayList;

public class State {
	
    private City currentCity;
    private City destinationCity;
    private boolean task;
    
    
	private static ArrayList<State> states = new ArrayList<State>();
	

    public State(City currentCity) {
        this.currentCity = currentCity;
        this.destinationCity = null;
        this.task = false;
    }
    
    public State(City currentCity, City destinationCity) {
        this.currentCity = currentCity;
        this.destinationCity = destinationCity;
        this.task = true;
    }
	
	public static void initStates(List<City> cities) {
		for (City city1 : cities) {
			for (City city2: cities) {
				if(city1 == city2) {
					State.states.add(new State(city1));
				} else {
					State.states.add(new State(city1, city2));
				}
			}
		}
	}
	
	public void setCurrentCity(City currentCity) {
		this.currentCity = currentCity;
	}

	public City getCurrentCity() {
		return currentCity;
	}
	
	public void setDestinationCity(City destinationCity) {
		this.destinationCity = destinationCity;
	}

	public City getDestinationCity() {
		return destinationCity;
	}
	
	public static ArrayList<State> getStates(){
		return states;
	}
	
	public boolean getTask() {
		return task;
	}

}

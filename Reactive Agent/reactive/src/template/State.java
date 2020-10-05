package template;

import logist.topology.Topology.City;
import java.util.List;
import java.util.ArrayList;

public class State {
	
    private City currentCity;
    private City destinationCity;
    private boolean task;
    
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
	
	public void setCurrentCity(City currentCity) {
		this.currentCity = currentCity;
	}

	public City getCurrentCity() {
		return currentCity;
	}
	
	public void setDestinationCity(City destinationCity) {
		this.destinationCity = destinationCity;
		this.task = true;
	}

	public City getDestinationCity() {
		return destinationCity;
	}
	
	public boolean getTask() {
		return task;
	}

}

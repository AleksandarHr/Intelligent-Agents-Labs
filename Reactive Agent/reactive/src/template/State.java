package template;

import logist.topology.Topology.City;
import java.util.List;
import java.util.Objects;
import java.util.ArrayList;

public class State {
	
    private City currentCity;
    private City destinationCity;
    
    public State(City currentCity) {
        this.currentCity = currentCity;
        this.destinationCity = null;
    }
    
    public State(City currentCity, City destinationCity) {
        this.currentCity = currentCity;
        this.destinationCity = destinationCity;
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
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currentCity == null) ? 0 : currentCity.hashCode());
		result = prime * result + ((destinationCity == null) ? 0 : destinationCity.hashCode());
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
		State other = (State) obj;
		if (currentCity == null) {
			if (other.currentCity != null)
				return false;
		} else if (!currentCity.equals(other.currentCity))
			return false;
		if (destinationCity == null) {
			if (other.destinationCity != null)
				return false;
		} else if (!destinationCity.equals(other.destinationCity))
			return false;
		return true;
	}
}

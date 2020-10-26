package template;

import logist.task.Task;
import logist.topology.Topology.City;

public class CentralizedAction {
	
	City currentCity;
	Task currentTask;
	actionType type;
	
	enum actionType{
		PICKUP,
		DELIVER
	}

	public CentralizedAction(City currentCity, Task currentTask, actionType type) {
		super();
		this.currentCity = currentCity;
		this.currentTask = currentTask;
		this.type = type;
	}

	public City getCurrentCity() {
		return currentCity;
	}

	public void setCurrentCity(City currentCity) {
		this.currentCity = currentCity;
	}

	public Task getCurrentTask() {
		return currentTask;
	}

	public void setCurrentTask(Task currentTask) {
		this.currentTask = currentTask;
	}

	public actionType getType() {
		return type;
	}

	public void setType(actionType type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((currentCity == null) ? 0 : currentCity.hashCode());
		result = prime * result + ((currentTask == null) ? 0 : currentTask.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		CentralizedAction other = (CentralizedAction) obj;
		if (currentCity == null) {
			if (other.currentCity != null)
				return false;
		} else if (!currentCity.equals(other.currentCity))
			return false;
		if (currentTask == null) {
			if (other.currentTask != null)
				return false;
		} else if (!currentTask.equals(other.currentTask))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	

}

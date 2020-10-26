package template;

import java.util.Comparator;

public class StateComparator implements Comparable {
	private State state;
	Double cost;
	public StateComparator(State state, Double cost) {
		this.state = state;
		this.cost = cost;
	}

	@Override
	public int compareTo(Object o1) {
		return this.cost.compareTo(((StateComparator)o1).cost);
	}

	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public Double getCost() {
		return cost;
	}

	public void setCost(Double cost) {
		this.cost = cost;
	}

}

package template;

import java.util.Comparator;

public class StateComparator implements Comparator<State> {
	
	public StateComparator() {
		
	}

	@Override
	public int compare(State o1, State o2) {
		// TODO Auto-generated method stub
		
		return Double.compare(o1.getHeuristics(), o2.getHeuristics());
	}

}

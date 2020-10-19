package template;
import logist.plan.Action;
import logist.task.Task;
import logist.topology.Topology.City;

public class Tuple {
	public enum Type { DELIVER, PICKUP }
	public Task task;
	public Type type;
	
	public Tuple(Type type, Task task) {
		this.task = task;
		this.type = type;
	}

}

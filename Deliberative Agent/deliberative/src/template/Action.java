package template;

import logist.task.Task;

public class Action {
	
	public enum Type { PICKUP, DELIVER }
	public Type type;
	public Task task;
	
	public Action(Type type, Task task) {
		this.type = type;
		this.task = task;
	}
}

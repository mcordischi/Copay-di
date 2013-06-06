package message;

import task.TaskID;

public class TaskErrorMessage extends TaskMessage {

	private Throwable t;
	private TaskID taskID;
	
	public TaskErrorMessage(Throwable t, TaskID taskID) {
		super(TaskMessage.MessageType.TASK_ERROR);
		this.t = t;
		this.taskID = taskID;
	}

	public Throwable getThrowable() {
		return t;
	}

	public void setThrowable(Throwable t) {
		this.t = t;
	}

	public TaskID getTaskID() {
		return taskID;
	}

	public void setTaskID(TaskID taskID) {
		this.taskID = taskID;
	}
	
	

}

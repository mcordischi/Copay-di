package message;

import task.TaskID;

public class TaskResultMessage extends TaskMessage {
	
	private Object result;
	private TaskID taskID;
	
	public TaskResultMessage(Object result,TaskID taskID) {
		super(TaskMessage.MessageType.TASK_RESULT);
		this.result = result;
		this.taskID = taskID;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	public TaskID getTaskID() {
		return taskID;
	}

	public void setTaskID(TaskID taskID) {
		this.taskID = taskID;
	}

	

}

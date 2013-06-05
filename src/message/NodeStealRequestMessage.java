package message;

public class NodeStealRequestMessage extends TaskMessage {

	int tasksRequested;
	
	public NodeStealRequestMessage(int tasksRequested) {
		super(MessageType.NODE_STEAL_REQUEST);
		this.tasksRequested = tasksRequested;
	}

	public int getTasksRequested() {
		return tasksRequested;
	}

	public void setTasksRequested(int tasksRequested) {
		this.tasksRequested = tasksRequested;
	}

	
}

package message;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.Serializable;

import org.jgroups.Message;
import org.jgroups.util.Streamable;



public class TaskMessage implements Serializable {
	public static enum MessageType {ADD_TASK, REMOVE_TASK, GLOBAL_PAUSE, GLOBAL_START, 
			TASK_RESULT, TASK_REQUEST, TASK_RESPONSE, TASK_STEAL, TASK_STATE};
	protected MessageType type;

	public TaskMessage(MessageType type){
		this.type = type;
	}
	


	public MessageType getType() {
		return type;
	}

	public void setType(MessageType type) {
		this.type = type;
	}
	
	

}

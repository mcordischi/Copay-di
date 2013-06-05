package message;

import java.util.Vector;

import task.TaskID;

/**
 * NOT IMPLEMENTED
 * The node that receives a STEAL_REQUEST just changes the Handler of the stolen tasks
 * and notifies the cluster.
 * 
 * Using this Class to notify the stealer has no advantages.
 * @author marto
 *
 */
public class NodeStealResponseMessage extends TaskMessage {

	Vector<TaskID> stolenTasks;
	
	public NodeStealResponseMessage() {
		super(MessageType.NODE_STEAL_RESPONSE);
	}

}

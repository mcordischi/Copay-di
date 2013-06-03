package node;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import message.NodeInfoMessage;
import message.TaskMessage;
import message.TaskNotificationMessage;
import node.NodeInformation.NodeType;


import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import event.Eventable;
import task.*;

public class TasksNode extends ReceiverAdapter implements Node {
	
	protected Eventable e;

	protected Vector<TaskEntry> tasksIndex = new Vector<TaskEntry>();

	
	protected Vector<NodeInformation> nodesInfo = new Vector<NodeInformation>();
	protected NodeInformation info;
	protected NodeType nodeType;
	
	protected JChannel channel;
	protected View actualView;
	
	private boolean globalState;
	private boolean localState;
	private boolean finished;
	

	
	public TasksNode(Eventable e){
		this.e = e;
	}
	
	@Override
	public void connect(String cluster) {
		try {
			channel = new JChannel();
			channel.setReceiver(this);
	        channel.connect(cluster);
	        channel.getState(null, 10000);
		} catch (Exception e1) {
			e.eventError("Could not connect to cluster");
			e1.printStackTrace();
		}
		info = new TasksNodeInformation(channel.getAddress(),nodeType,localState);
		sendInformation();
	}

	/**
	 * Disconnect from the cluster
	 */
	public void disconnect(){
		channel.close();
	}
	
	@Override
	public boolean getGlobalState() {
		return globalState;	
	}
	
	public void setGlobalState(boolean globalState){
		this.globalState = globalState;
		try {
			if (globalState)
				channel.send(null,new TaskMessage(TaskMessage.MessageType.GLOBAL_START));
			else
				channel.send(null,new TaskMessage(TaskMessage.MessageType.GLOBAL_PAUSE));
		} catch (Exception e1) {
			e.eventError("Could not set Global State. Are you still connected to the cluster?");
		}
	}
	
	@Override
	public void receive(Message msg){
		TaskMessage tmsg = (TaskMessage) msg.getObject();
		switch (tmsg.getType()){
		case ADD_TASK:
			handleAddTask(((TaskNotificationMessage)tmsg).getEntry());
			break;
		case REMOVE_TASK :
			handleRemoveTask(((TaskNotificationMessage)tmsg).getEntry());
			break;
		case GLOBAL_START :
			globalState= WORKING;
			e.eventSystemResume();
			break;
		case GLOBAL_PAUSE :
			globalState= PAUSE;
			e.eventSystemPause();
			break;
		case TASK_STATE :
			if (!msg.getSrc().equals(channel.getAddress()))
				handleTaskUpdate(((TaskNotificationMessage)tmsg).getEntry());
			break;
		case NODE_INFORMATION:
			updateNodeInformation((NodeInfoMessage)tmsg);
			break;
		case INFORMATION_REQUEST:
			sendInformation();
			break;
		default:
			break;
		}	
	}

	
	


	/**
	 * Actions to make when a TASK_UPDATE message was received
	 * @param entry
	 */
	private void handleTaskUpdate(TaskEntry entry){
		TaskEntry oldEntry = null;
		synchronized(tasksIndex){
			for(TaskEntry e : tasksIndex)
				if (e.equals(entry)){
					oldEntry = e;
					break;
				}
		}
		if (oldEntry == null)
			//The ADD_TASK message hasn't been received yet
			handleAddTask(entry);
		else {
			if (oldEntry.getState() != TaskEntry.StateType.FINISHED){
				oldEntry.setState(entry.getState());
				e.eventTaskUpdate(entry);
			}
		}
	}
	
	/**
	 * Handle the ADD_TASK message
	 * @param entry
	 */
	private void handleAddTask(TaskEntry entry){
		boolean exists = tasksIndex.contains(entry);
		if (!exists){
			synchronized(tasksIndex){
			tasksIndex.add(entry);
			}
			this.setFinished(false);
			e.eventNewTask(entry);
		}
	}
	
	/**
	 * Handle the REMOVE_TASK message
	 */
	protected void handleRemoveTask(TaskEntry entry){
		synchronized(tasksIndex){
			tasksIndex.remove(entry);
		}
		e.eventRemoveTask(entry.getId());
	}
	
	/**
	 * Handles the update of a node's information.
	 * @param tmsg
	 */
	private void updateNodeInformation(NodeInfoMessage tmsg) {
		NodeInformation i = tmsg.getInfo();
		synchronized(nodesInfo){
			if (!nodesInfo.contains(i))
				nodesInfo.add(i);
			else
				for(NodeInformation ni : nodesInfo)
					if (ni.equals(i))
						ni.update(i);
		}
	}
	
	/**
	 * Sends updated information about the Node to the cluster
	 */
	public void sendInformation(){
		try{
			channel.send(null,new NodeInfoMessage(info));
		} catch (Exception e1){
			e.eventError("Information Response failed. Are you still connected to the cluster?");
		}
	}

	@Override
	public void getState(OutputStream output) throws Exception {
		DataOutputStream out = new DataOutputStream(output);
		synchronized(tasksIndex){
			Util.objectToStream(tasksIndex, out);
		}
		
		synchronized(nodesInfo){
			Util.objectToStream(nodesInfo, out);
		}
		
		Util.objectToStream(globalState, out);
		e.eventWarning(info.getAddress() + " is sending info");
	}
	
	@Override
	public void setState(InputStream input) throws Exception {
		Vector<TaskEntry> inEntryVector = (Vector<TaskEntry>)Util.objectFromStream(new DataInputStream(input));
		Vector<NodeInformation> inInfoVector = (Vector<NodeInformation>)Util.objectFromStream(new DataInputStream(input));
		if (inEntryVector != null && inInfoVector != null)
			globalState = (boolean)Util.objectFromStream(new DataInputStream(input));
		synchronized(tasksIndex){
			tasksIndex.addAll(inEntryVector);
		}
		synchronized(nodesInfo){
			nodesInfo.addAll(inInfoVector);
		}
		e.eventWarning(info.getAddress() + " is receiving info");
	}
	
//GETTERS or SETTERS	
	
	
	/**
	 * Return a Vector cotaining the TaskEntries that matches the owner and handler 
	 * @param owner
	 * @param handler
	 * @return
	 */
	protected synchronized Vector<TaskEntry> getTasks(Address owner,Address handler){
		Vector<TaskEntry> result = new Vector<TaskEntry>();
		for (TaskEntry e : tasksIndex)
			// Add to the result if the owner is the same and if the handlers are null or the same
			if (e.getOwner().equals(owner)) 
				if (handler == null && e.getHandler() == null)
					result.add(e);
				else if (handler != null && e.getHandler() != null && e.getHandler().equals(handler))
					result.add(e);
		return result;
	}
	
		
	public boolean isLocalState() {
		return localState;
	}

	public void setLocalState(boolean localState) {
		this.localState = localState;
	}
	
	public boolean getLocalState(){
		return localState;
	}
	
	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}
	
	//DEBUG METHODS
	
	public void notifyInformation(){
		synchronized(nodesInfo){
			for (NodeInformation i : nodesInfo)
				e.eventInformation(i);
		}
	}
	
	public void notifyView(){
		for (Address a : channel.getView())
			e.eventNodeAvailable(a);
	}
	
	
}

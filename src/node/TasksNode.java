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
import event.NullEventInterface;
import task.*;

public class TasksNode extends ReceiverAdapter implements Node {
	
	protected Eventable e;

	protected Vector<TaskEntry> tasksIndex = new Vector<TaskEntry>();

	
	protected Vector<NodeInformation> nodesInfo = new Vector<NodeInformation>();
	protected NodeInformation info;
	protected NodeType nodeType;
	
	protected int maxThreads;
	
	protected JChannel channel;
	protected View actualView;
	
	private boolean globalState;
	private boolean localState;
	private boolean finished;
	

	
	public TasksNode(Eventable e, boolean initialLocalState){
		if (e==null)
			this.e = new NullEventInterface();
		else
			this.e = e;
		nodeType = NodeType.UNDEFINED;
		maxThreads = 0;
		finished = false;
		localState = initialLocalState;
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
	
	public void setSystemState(boolean globalState){
		boolean oldState = getGlobalState();
		setGlobalState(globalState);
		try {
			if (oldState != globalState){
				if (globalState)
					channel.send(null,new TaskMessage(TaskMessage.MessageType.GLOBAL_START));
				else
					channel.send(null,new TaskMessage(TaskMessage.MessageType.GLOBAL_PAUSE));
			}
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
			setGlobalState(WORKING);
			break;
		case GLOBAL_PAUSE :
			setGlobalState(PAUSE);
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
	protected void handleTaskUpdate(TaskEntry entry){
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
				oldEntry.setHandler(entry.getHandler());
				e.eventTaskUpdate(entry);
			}
		}
		if (entry.getHandler().equals(info.getAddress()) || entry.getOwner().equals(info.getAddress()))
			setFinished(false);
	}
	
	/**
	 * Handle the ADD_TASK message
	 * @param entry
	 */
	protected void handleAddTask(TaskEntry entry){
		boolean exists = tasksIndex.contains(entry);
		if (!exists){
			synchronized(tasksIndex){
			tasksIndex.add(entry);
			}
			this.setFinished(false);
			e.eventNewTask(entry);
		}
		if (entry.getHandler().equals(info.getAddress()) || entry.getOwner().equals(info.getAddress()))
			finished = false;
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
			boolean contains = false;
			for(NodeInformation ni : nodesInfo)
				if (ni.equals(i)){
					ni.update(i);
					contains = true;
					break;
				}
			if (!contains)
				nodesInfo.add(i);
		}
	}
	
	/**
	 * Sends updated information about the Node to the cluster
	 */
	public void sendInformation(){
		if (info == null)
			info = new TasksNodeInformation(channel.getAddress(),nodeType,localState,maxThreads);
		try{
			channel.send(null,new NodeInfoMessage(info));
		} catch (Exception e1){
			e.eventError("Information send failed. Are you still connected to the cluster?");
		}
	}

	/**
	 * WARNING, NOT syncrhonized structures.
	 */
	@Override
	public void getState(OutputStream output) throws Exception {
		DataOutputStream out = new DataOutputStream(output);
		Util.objectToStream(tasksIndex, out);
		Util.objectToStream(nodesInfo, out);
		Util.objectToStream(globalState, out);
	}
	
	/**
	 * WARNING, NOT syncrhonized structures.
	 */
	@Override
	public void setState(InputStream input) throws Exception {
		Vector<TaskEntry> inEntryVector = (Vector<TaskEntry>)Util.objectFromStream(new DataInputStream(input));
		Vector<NodeInformation> inInfoVector = (Vector<NodeInformation>)Util.objectFromStream(new DataInputStream(input));
		if (inEntryVector != null && inInfoVector != null)
			globalState = (boolean)Util.objectFromStream(new DataInputStream(input));
		updateState(inEntryVector,inInfoVector);
	}
	
	private void updateState(Vector<TaskEntry> inEntry, Vector<NodeInformation> inInfo){
		synchronized(tasksIndex){
			tasksIndex.addAll(inEntry);
		}
		synchronized(nodesInfo){
			nodesInfo.addAll(inInfo);
		}
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
		if (this.localState != localState){
			this.localState = localState;
			e.eventLocalState(localState);
		}
	}
	
	/**
	 * Protected and internal method for setting the global state without notifying the entire system.
	 * Used when a notification was received.
	 * @param state
	 */
	protected void setGlobalState(boolean state) {
		if (this.globalState != state){
			this.globalState = state;
			e.eventSystemState(state);
		}
	}
	
	public boolean getLocalState(){
		return localState;
	}
	
	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		if (finished && !this.finished)
			e.eventLocalCompletion();
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
	
	public void forceUpdate() throws Exception{
		info.forceUpdate(channel);
	}
	
	public void notifyTasksIndex(){
		if (tasksIndex.size() == 0)
			e.eventWarning("The tasks Index is empty");
		for (TaskEntry entry : tasksIndex)
			e.notifyTask(entry);
	}
	
}

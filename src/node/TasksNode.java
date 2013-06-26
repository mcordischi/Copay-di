package node;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

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
import task.TaskEntry.StateType;


/**
 * This class contains all the basic functionality needed for task Support. Slaves and Masters must inherit from this and override 
 * some methods, adding specific functionality.
 * 
 * It also implements the Monitor interface, to avoid the creation of an almost useless class.
 * 
 * Pending jobs
 * - Improve NodeInformation usage 
 * 
 * @author marto
 *
 */
public class TasksNode extends ReceiverAdapter implements Node,Monitor {
	
	protected Eventable e;

	protected Vector<TaskEntry> tasksIndex = new Vector<TaskEntry>();
	protected final Semaphore tasksIndexSem = new Semaphore(1);
	
	protected Vector<NodeInformation> nodesInfo = new Vector<NodeInformation>();
	protected NodeInformation info;
	protected NodeType nodeType;
	
	protected int maxThreads;
	
	protected JChannel channel;
	private View actualView;
	protected final Semaphore viewSem = new Semaphore(1); 
	
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
	public boolean getSystemState() {
		return globalState;	
	}
	
	public void setSystemState(boolean globalState){
		boolean oldState = getSystemState();
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
		final Message msgFinal = msg;
		final TaskMessage tmsg = (TaskMessage) msg.getObject();
		switch (tmsg.getType()){
		case ADD_TASK:
			new Thread() {
				@Override
				public void run() {
					handleAddTask(((TaskNotificationMessage)tmsg).getEntry());
				}
			}.start();
			break;
		case REMOVE_TASK :
			new Thread() {
				@Override
				public void run() {
					handleRemoveTask(((TaskNotificationMessage)tmsg).getEntry());
				}
			}.start();
			break;
		case GLOBAL_START :
			setGlobalState(WORKING);
			break;
		case GLOBAL_PAUSE :
			setGlobalState(PAUSE);
			break;
		case TASK_STATE :
			new Thread() {
				@Override
				public void run() {
					if (!msgFinal.getSrc().equals(channel.getAddress()))
						handleTaskUpdate(((TaskNotificationMessage)tmsg).getEntry());
				}
			}.start();
			break;
		case NODE_INFORMATION:
			new Thread() {
				@Override
				public void run() {
					updateNodeInformation((NodeInfoMessage)tmsg);
				}
			}.start();
			break;
		case INFORMATION_REQUEST:
			new Thread() {
				@Override
				public void run() {
					sendInformation();
				}
			}.start();
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
			try {
				tasksIndexSem.acquire();
			} catch (InterruptedException e1) {
				e.eventError("Internal error - Semaphores");
			}
			for(TaskEntry e : tasksIndex)
				if (e.equals(entry)){
					oldEntry = e;
					break;
				}
			tasksIndexSem.release();
		}
		if (oldEntry == null)
			;
			//The ADD_TASK message hasn't been received yet
			//WARNING - This branch commented due to synchronization problems.
			//After a Master crashes, some TASK_UPDATE messages may arrive, and they must be ignored. 
			//This is the easiest way to solve this problem. 
			//handleAddTask(entry);
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
		synchronized(tasksIndex){
			try {
				tasksIndexSem.acquire();
			} catch (InterruptedException e1) {
				e.eventError("Internal Error - Semaphores");
			}
			boolean exists = tasksIndex.contains(entry);
			if (!exists){
				tasksIndex.add(entry);
			}
			tasksIndexSem.release();
		}
			this.setFinished(false);
			e.eventNewTask(entry);
		if (entry.getHandler().equals(info.getAddress()) || entry.getOwner().equals(info.getAddress()))
			finished = false;
	}
	
	/**
	 * Handle the REMOVE_TASK message
	 */
	protected void handleRemoveTask(TaskEntry entry){
		synchronized(tasksIndex){
			try {
				tasksIndexSem.acquire();
			} catch (InterruptedException e1) {
				e.eventError("Internal Error - Semaphores");
			}
			tasksIndex.remove(entry);
			tasksIndexSem.release();
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


	@Override
	public void getState(OutputStream output) throws Exception {
		DataOutputStream out = new DataOutputStream(output);
		synchronized(tasksIndex){
			try {
				tasksIndexSem.acquire();
			} catch (InterruptedException e1) {
				e.eventError("Internal error - Semaphores");
			}
			Util.objectToStream(tasksIndex, out);
			tasksIndexSem.release();
		}
		synchronized(nodesInfo){
			Util.objectToStream(nodesInfo, out);
		}
		Util.objectToStream(globalState, out);
	}
	
	/**
	 * 
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
			try {
				tasksIndexSem.acquire();
			} catch (InterruptedException e1) {
				e.eventError("Internal error - Semaphores");
			}
			tasksIndex.addAll(inEntry);
			tasksIndexSem.release();
		}
		synchronized(nodesInfo){
			nodesInfo.addAll(inInfo);
		}
	}
	
	/**
	 * Must do some work when a node crashes. It creates a new thread that executes the method {@link #handleNodeCrash(Address)}, 
	 * because it may be a long running action
	 */
	public void viewAccepted(final View new_view) {
		new Thread() {
			@Override
			public void run() {
				try {
					viewSem.acquire();
				} catch (InterruptedException e1) {
					e.eventError("Problem with view Semaphore");
				}
				if (actualView != null){
					if (new_view.size() < actualView.size()){
						Address missingNode = searchMissingNode(new_view,actualView);
						if (missingNode != null)
						//run handleNodeCrash in a new thread, it may get long
								handleNodeCrash(missingNode);
						else
							e.eventError("Unidentified crash");
						}
						else{
							Address newNode = searchNewNode(new_view,actualView);
							e.eventNodeAvailable(newNode);
						}
					}
				actualView = new_view;
				viewSem.release();
			}
		}.start();
	}
	
	/**
	 * Called by {@link #viewAccepted(View)}, it handles tasks of the crashed node.
	 * @param missingNode
	 */
	protected synchronized void handleNodeCrash(Address missingNode){
				editTasks(missingNode);
				synchronized(nodesInfo){
					for(NodeInformation i : nodesInfo)
						if (i.getAddress().equals(missingNode)){
							nodesInfo.remove(i);
							break;
						}
				}
				e.eventNodeCrash(missingNode);		
	}
	
	
	/**
	 * Refresh the tasksIndex. If the node that crashed was a owner, the task is removed, if it was the
	 * responsible for a task, it sets the handler to null.
	 * @param address
	 */
	protected void editTasks(Address address){
		if (address != null){
			e.eventWarning("Editing tasks from " + address.toString());
			synchronized(tasksIndex){
				try {
					tasksIndexSem.acquire();
				} catch (InterruptedException e1) {
					e.eventError("Internal error - Semaphores");
				}
				for(int i=0 ; i<tasksIndex.size(); i++){
					TaskEntry te = tasksIndex.get(i);
					if (te.getOwner().equals(address)){
						tasksIndex.remove(te);
						i--;
					}
					else if (te.getHandler() != null && te.getHandler().equals(address)  && te.getState() != StateType.FINISHED){
						te.setHandler(null);
						te.setState(TaskEntry.StateType.SUBMITTED);
					}
				}
				tasksIndexSem.release();
			}
		}
	}
	
	
	/**
	 * Searches for the missing address and returns it
	 * @param newView
	 * @param oldView
	 * @return the missing address
	 */
	private Address searchMissingNode(View newView,View oldView){
		List<Address> n = newView.getMembers();
		List<Address> o = oldView.getMembers();
		for (int i=0;i<o.size();i++)
			if (!n.contains(o.get(i)))
				return o.get(i);
		e.eventWarning("Could not find missing node:\n");
		String str1 = new String();
		for (Address a : n)
			str1 += a.toString() + " ";
		String str2 = new String();
		for (Address a : o)
			str2 += a.toString() + " ";
		e.eventWarning("New List: " + str1);
		e.eventWarning("Old List: " + str2);
		return null;
	}
	
	/**
	 * Searches for the new address and returns it
	 * @param newView
	 * @param oldView
	 * @return the new address
	 */
	private Address searchNewNode(View newView,View oldView){
		List<Address> n = newView.getMembers();
		List<Address> o = oldView.getMembers();
		for (int i=0;i<n.size();i++)
			if (n.get(i).equals(o.get(i)))
				return n.get(i);
		return null;
	}
	
	
	
//GETTERS or SETTERS	
	
	
	/**
	 * Return a Vector containing the TaskEntries that matches the owner and handler 
	 * @param owner
	 * @param handler
	 * @return
	 */
	protected synchronized Vector<TaskEntry> getTasks(Address owner,Address handler){
		Vector<TaskEntry> result = new Vector<TaskEntry>();
		synchronized(tasksIndex){
			try {
				tasksIndexSem.acquire();
			} catch (InterruptedException e1) {
				e.eventError("Internal error - Semaphores");
			}
			for (TaskEntry e : tasksIndex)
				// Add to the result if the owner is the same and if the handlers are null or the same
				if (e.getOwner().equals(owner)) 
					if (handler == null && e.getHandler() == null)
						result.add(e);
					else if (handler != null && e.getHandler() != null && e.getHandler().equals(handler))
						result.add(e);
			tasksIndexSem.release();
		}
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
		try {
			tasksIndexSem.acquire();
		} catch (InterruptedException e1) {
			e.eventError("Internal error - Semaphores");
		}
		if (tasksIndex.size() == 0)
			e.eventWarning("The tasks Index is empty");
		for (TaskEntry entry : tasksIndex)
			e.notifyTask(entry);
		tasksIndexSem.release();
	}

	@Override
	public View getClusterInfo() {
		return actualView;
	}

	@Override
	public Collection<TaskEntry> getTasksInfo() {
		return tasksIndex;
	}

	@Override
	public Collection<NodeInformation> getNodeInfo() {
		return nodesInfo;
	}
	
}

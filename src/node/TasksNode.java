package node;

import java.util.Vector;

import message.TaskMessage;
import message.TaskNotification;


import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import event.Eventable;
import task.*;

public class TasksNode extends ReceiverAdapter implements Node {
	
	protected Eventable e;

	protected Vector<TaskEntry> tasksIndex = new Vector<TaskEntry>();
	
	protected JChannel channel;
	protected View actualView;
	
	private boolean globalState;
	private boolean localState;
	private boolean finished;
	
	public static boolean WORKING = true ;
	public static boolean PAUSE = false ;
	
	
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
		}
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
	
	
	@Override
	public void receive(Message msg){
		TaskMessage tmsg = (TaskMessage) msg.getObject();
		switch (tmsg.getType()){
		case ADD_TASK:
			handleAddTask(((TaskNotification)tmsg).getEntry());
			break;
		case REMOVE_TASK :
			handleRemoveTask(((TaskNotification)tmsg).getEntry());
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
				handleTaskUpdate(((TaskNotification)tmsg).getEntry());
			break;
		default:
			break;
		}	}

	
	
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
	private void handleRemoveTask(TaskEntry entry){
		synchronized(tasksIndex){
			tasksIndex.remove(entry);
		}
//		TODO Send to SlaveNode
//		synchronized(pendingTasks){
//			pendingTasks.remove(entry);
//		}
//		synchronized(workingTasks){
//			if (workingTasks.containsKey(entry)){
//				Future taskF = workingTasks.get(entry);
//				taskF.cancel(true);
//				workingTasks.remove(entry);
//			}
//		}
		e.eventRemoveTask(entry.getId());
	}

	
	public boolean isLocalState() {
		return localState;
	}

	public void setLocalState(boolean localState) {
		this.localState = localState;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setFinished(boolean finished) {
		this.finished = finished;
	}
	
	
}

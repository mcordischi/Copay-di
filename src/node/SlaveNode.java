package node;

import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import message.NodeStealRequestMessage;
import message.TaskErrorMessage;
import message.TaskMessage;
import message.TaskMessage.MessageType;
import message.TaskNotificationMessage;
import message.TaskRequestMessage;
import message.TaskResponseMessage;
import message.TaskResultMessage;
import node.NodeInformation.NodeType;

import org.jgroups.Address;
import org.jgroups.Message;

import event.Eventable;

import task.Task;
import task.TaskEntry;
import task.TaskEntry.StateType;
import task.TaskID;

import algorithm.TaskStealingStrategy;


/**
 * Abstract Class that implements all the methods for the SlaveNode,
 * except from start().
 * @author marto
 *
 */
public abstract class SlaveNode extends TasksNode implements Slave {

	protected final ExecutorService executor;
	

	protected Vector<TaskEntry> pendingTasks = new Vector<TaskEntry>();
	protected Map<TaskEntry,Future<Object> > workingTasks = new ConcurrentHashMap<TaskEntry, Future<Object> >();
	
	
	public SlaveNode(Eventable e, int maxThreads) {
		super(e);
		this.maxThreads = maxThreads;
		this.executor = new ScheduledThreadPoolExecutor(maxThreads);
		nodeType = NodeType.SLAVE;
	}
	
	
	/**
	 * This method contain the task fetching and/or stealing
	 */
	protected abstract void start();
	
	
	
	/**
	 * Sends a request to the owner of the task, asking for the task. The answer is handle in {@link #receive(Message)}.
	 * @param entry
	 * @param task
	 */
	public void requestTask(TaskEntry entry){
		entry.setState(TaskEntry.StateType.WORKING);
		e.eventTaskRequest(entry.getId());
		synchronized(pendingTasks){
			pendingTasks.add(entry);
		}
		try {
			//Notify cluster that the node is going to start the execution of the task
			channel.send(null, new TaskNotificationMessage(TaskMessage.MessageType.TASK_STATE,entry));
			//Request the task to the owner
			channel.send(entry.getOwner(), new TaskRequestMessage(TaskMessage.MessageType.TASK_REQUEST, entry.getId()));
		} catch (Exception e1) {
			e.eventError("Request Task Failed. Are you still connected to the cluster?");
			synchronized(entry){
				entry.setState(TaskEntry.StateType.UNDEFINED);
			}
			synchronized(pendingTasks){
				pendingTasks.remove(entry);
			}
		}

	}
	
	/**
	 * calls start() if new local state is true
	 */
	@Override
	public void setLocalState(boolean localState) {
		boolean oldLocalState = getLocalState();
		super.setLocalState(localState);
		if( oldLocalState == PAUSE && !isFinished() && localState == WORKING)
			start();
	}

	
	@Override
	protected void setGlobalState(boolean state){
		super.setGlobalState(state);
		start();
	}

	@Override
	public void receive(Message msg) {
		TaskMessage tmsg = (TaskMessage) msg.getObject();
		switch (tmsg.getType()){
		case TASK_RESPONSE:
			TaskID tId = ((TaskResponseMessage)tmsg).getId();
			Task task = ((TaskResponseMessage)tmsg).getTask() ;
			if (task == null)
				e.eventError("Null task received");
			else
				handleTask(tId, task);
			break;
		case NODE_STEAL_REQUEST:
			handleNodeSteal((NodeStealRequestMessage)tmsg, msg.getSrc());
		default:
			super.receive(msg);
		}
	}
	

	/**
	 * Handles the task and calls {@link #sendResult(Object, TaskEntry)} to return the result via multicast message
	 * @throws Exception 
	 */
	protected void handleTask(TaskID id, Task task){
		TaskEntry entry = null;
		synchronized(pendingTasks){
			for (TaskEntry i : pendingTasks){
				if (i.getId().equals(id)){
					entry = i;
					break;
				}
			}
		}
		if (entry != null){
			synchronized(pendingTasks){
				pendingTasks.remove(entry);
			}
			e.eventTaskExecution(entry);
			Future<Object> fResult = (Future<Object>) executor.submit(task);
			workingTasks.put(entry, fResult);
			Object result;
			try {
				result = fResult.get();
			} catch (Exception exc){
				sendException(exc, entry);
				return;
			}
			try {
				sendResult(result,entry);
			} catch (Exception e1) {
				e.eventError("Problem sending the result");
			}
		}
		else
			e.eventWarning(" Received task " + id.toString() + ", but not in pendingTasks");
	}
	
	
	/**
	 * Handle the REMOVE_TASK message
	 */
	@Override
	protected void handleRemoveTask(TaskEntry entry){
		synchronized(pendingTasks){
			pendingTasks.remove(entry);
		}
		synchronized(workingTasks){
			if (workingTasks.containsKey(entry)){
				Future taskF = workingTasks.get(entry);
				taskF.cancel(true);
				workingTasks.remove(entry);
			}
		}
		super.handleRemoveTask(entry);
	}
	
	/**
	 * Handle the NODE_STEAL_REQUEST
	 * Searches for pending tasks that are assigned to itself and updates them to 
	 * @param src
	 */
	protected void handleNodeSteal(NodeStealRequestMessage m, Address stealer){
		ArrayList<TaskEntry> array = new ArrayList<TaskEntry>();
		int qty = m.getTasksRequested();
		synchronized(tasksIndex){
			for (TaskEntry t : tasksIndex)
				if (array.size() == qty)
					break;
				else if (t.getHandler().equals(info.getAddress()) && 
						(t.getState() == StateType.SUBMITTED || t.getState() == StateType.UNDEFINED )){
					array.add(new TaskEntry(t.getId(),stealer));
					
				}
		}
		e.eventNodeStealResponse(array.size() > 0, info.getAddress(), stealer);
		for (TaskEntry t : array){
			try {
				channel.send(null, new TaskNotificationMessage(MessageType.TASK_STATE, t));
			} catch (Exception e1) {
				e.eventError("Couldn't make an update on a Task state. Are you still connected to the cluster?");
			}
		}
	}
	
	
	/**
	 * Send a multicast message with the FINISHED notification of a task.
	 * Also sends an unicast message to the Task's Owner with the result
	 * @param result: the result of the task
	 * @param entry: the task entry
	 */
	protected void sendResult(Object result, TaskEntry entry ){
		synchronized(entry){
			entry.setState(TaskEntry.StateType.FINISHED);
		}
		e.eventTaskComplete(entry);
		//Notifies the cluster of the state and the result and sends to the owner the result
		try {
			channel.send(null, new TaskNotificationMessage(TaskMessage.MessageType.TASK_STATE,entry));
			channel.send(entry.getOwner(), new TaskResultMessage(result, entry.getId()));
		} catch (Exception e1) {
			e.eventError("Send result failed. Are you still connected to the cluster?");
		}
	}
	
	//Almost a copy-paste of sendResult. TODO merge them.
	/**
	 * Send a multicast message with the FINISHED notification of a task.
	 * Also sends an unicast message to the Task's Owner with the Exception.
	 * 
	 * @param exc
	 * @param entry
	 */
	protected void sendException(Exception exc, TaskEntry entry){
		synchronized(entry){
			entry.setState(TaskEntry.StateType.FINISHED);
		}
		e.eventTaskComplete(entry);
		//Notifies the cluster of the state and the result and sends to the owner the result
		try {
			channel.send(null, new TaskNotificationMessage(TaskMessage.MessageType.TASK_STATE,entry));
			channel.send(entry.getOwner(), new TaskErrorMessage(exc, entry.getId()));
		} catch (Exception e1) {
			e.eventError("Send Exception failed. Are you still connected to the cluster?");
		}
	}
	
}

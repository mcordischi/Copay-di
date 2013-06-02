package node;

import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import message.TaskMessage;
import message.TaskNotification;
import message.TaskResponse;

import org.jgroups.Message;

import event.Eventable;

import task.Task;
import task.TaskEntry;
import task.TaskID;

import algorithm.StealingStrategy;


/**
 * Abstract Class that implements all the methods for the SlaveNode,
 * except from start().
 * @author marto
 *
 */
public abstract class SlaveNode extends TasksNode implements Slave {

	private final ExecutorService executor;
	private int maxThreads;
	

	private Vector<TaskEntry> pendingTasks = new Vector<TaskEntry>();
	private Map<TaskEntry,Future<Object> > workingTasks = new ConcurrentHashMap<TaskEntry, Future<Object> >();
	
	
	public SlaveNode(Eventable e, int maxThreads) {
		super(e);
		this.maxThreads = maxThreads;
		this.executor = new ScheduledThreadPoolExecutor(maxThreads);
	}
	
	
	/**
	 * This method contain the task fetching and/or stealing
	 */
	protected abstract void start();
	
	
	/**
	 * calls start() if new local state is true
	 */
	@Override
	public void setLocalState(boolean localState) {
		// TODO Auto-generated method stub

	}


	@Override
	public void receive(Message msg) {
		TaskMessage tmsg = (TaskMessage) msg.getObject();
		switch (tmsg.getType()){
		case TASK_RESPONSE:
			TaskID tId = ((TaskResponse)tmsg).getId();
			Task task = ((TaskResponse)tmsg).getTask() ;
			if (task == null)
				e.eventError("Null task received");
			else
				handleTask(tId, task);
			break;
		}
	}
	

	/**
	 * Handles the task and calls {@link #sendResult(Object, TaskEntry)} to return the result via multicast message
	 * @throws Exception 
	 */
	public void handleTask(TaskID id, Task task){
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
			try {
				sendResult(fResult.get(),entry);
			} catch (Exception e1) {
				e.eventError("Problem sending the result");
			}
		}
		else
			e.eventWarning(" Received task " + id.toString() + ", but not in pendingTasks");
	}
	
	
	
	/**
	 * Send a multicast message with the result of a task
	 * @param result: the result of the task
	 * @param entry: the task entry
	 */
	public void sendResult(Object result, TaskEntry entry ){
		synchronized(entry){
			entry.setState(TaskEntry.StateType.FINISHED);
		}
		e.eventTaskComplete(entry);
		//Notifies the cluster of the state and the result
		try {
			channel.send(null, new TaskNotification(TaskMessage.MessageType.TASK_RESULT,entry));
		} catch (Exception e1) {
			e.eventError("Send result failed. Are you still connected to the cluster?");
		}

		//TODO send Result to owner
	}
	
}

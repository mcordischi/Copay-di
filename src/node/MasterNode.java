package node;


import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;


import message.*;
import message.TaskMessage.MessageType;
import node.NodeInformation.NodeType;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;

import event.Eventable;

import algorithm.SchedulerStrategy;

import task.*;
import task.TaskEntry.StateType;

/**
 * Class implementing Master interface. Inherits from {@link TasksNode} the basic node's functionality. 
 * When submitting a {@link Task}, it uses a given {@link SchedulerStrategy} to assign a handler. If a Handler crashes, 
 * it will also use the strategy to re assign handler.  
 *  
 * @author marto
 *
 */
public class MasterNode extends TasksNode implements Master {
	

	private SchedulerStrategy schStrat;
	private Map<TaskID,FutureTaskResult> tasksResults = new ConcurrentHashMap<TaskID,FutureTaskResult>();
	private Map<TaskID,Task> tasksMap = new ConcurrentHashMap<TaskID,Task>();


	
	
// ******************************
//	METHODS
// ******************************
	
	public MasterNode(Eventable e,SchedulerStrategy schStrat){
		super(e,PAUSE);
		this.schStrat = schStrat;
//		setFinished(true);
		nodeType = NodeType.MASTER;
	}


	public MasterNode(Eventable e){
		super(e,PAUSE);
		this.schStrat = null;
//		setFinished(true);
		nodeType = NodeType.MASTER;
	}

	@Override
	protected void handleNodeCrash(Address missingNode) {
		super.handleNodeCrash(missingNode);
		//Schedule tasks that has no handler and the owner is this
		scheduleTasks(getTasks(channel.getAddress(),null));
	}
	

	@Override
	public FutureTaskResult<Object> submit(Task t) {
		//Create task ID and Entry
		TaskID id = TaskID.newTask(channel.getAddress());
		tasksMap.put(id, t);
		TaskEntry entry = new TaskEntry(id,null);
		//Schedule the task
		synchronized(nodesInfo){
			schStrat.assign(entry, nodesInfo);
		}
		
		//Notify the cluster that a new task exists
		try {
			channel.send(null, new TaskNotificationMessage(TaskMessage.MessageType.ADD_TASK,entry));
		} catch (Exception e1) {
			e.eventError("Submit Failed. Are you connected to the cluster?");
			tasksMap.remove(id);
			return null;
		}
		
		//Return a future
		FutureTaskResult<Object> futureTask = new FutureTaskResult<Object>(id,this);
		tasksResults.put(id, futureTask);
		return futureTask;
	}

	
	@Override
	public void cancel(TaskID id) {
		synchronized(tasksMap){
			tasksMap.remove(id);
		}
		synchronized(tasksResults){
//			tasksResults.get(id).cancel(true);
			tasksResults.remove(id);
		}
		
		try{
			//Creates a new TaskEntry to avoid looking for the real Entry in tasksIndex
			channel.send(null, new TaskNotificationMessage(MessageType.REMOVE_TASK,new TaskEntry(id,channel.getAddress())));
		} catch (Exception e1){
			e.eventError("Submit Failed. Are you connected to the cluster?");
		}
	}

	/**
	 * Assigns a handler to ALL the tasks that the node owns.
	 */
	private void scheduleTasks(){
			scheduleTasks(tasksIndex);
	}

	/**
	 * Assigns a handler to the @tasks that the node owns.
	 * Uses the Scheduler Strategy.
	 * Blocks tasksIndex for a safe usage.
	 * @param tasks : A vector of the tasks to schedule
	 */
	private void scheduleTasks(Vector<TaskEntry> tasks){
		synchronized(tasksIndex){
			try {
				tasksIndexSem.acquire();
			} catch (InterruptedException e1) {
				e.eventError("Internal Erorr - Semaphores");
			}
				synchronized(nodesInfo){
					schStrat.assign(tasks,nodesInfo);
				}
		//Notify the cluster that the tasks changed
			try {
				for (TaskEntry entry :tasks)
					if (entry.getOwner().equals(info.getAddress()))
						channel.send(null, new TaskNotificationMessage(TaskMessage.MessageType.TASK_STATE,entry));
					} catch (Exception e1) {
						e.eventError("Submit Failed. Are you connected to the cluster?");
					}
			tasksIndexSem.release();
		}
		
	}
	
	@Override
	public void receive(Message msg) {
		final Message msgFinal = msg ;
		final TaskMessage tmsg = (TaskMessage) msg.getObject();
		switch (tmsg.getType()){
		case TASK_RESULT :
			new Runnable() {
				@Override
				public void run() {
				handleTaskResult(((TaskResultMessage)tmsg));
				}
			}.run();
			break;
		case TASK_REQUEST :
			new Runnable() {
				@Override
				public void run() {
					TaskID id = ((TaskRequestMessage)tmsg).getId();
					taskResponse(id,msgFinal.getSrc());
				}
			}.run();
			break;
		case TASK_ERROR :
			new Runnable() {
				@Override
				public void run() {
					handleTaskError((TaskErrorMessage)tmsg);
				}
			}.run();
			break;
		default:
			super.receive(msg);
		}
	}
		
	
	/**
	 * Handle the TASK_RESULT message
	 * TASK_RESULT contains the result of the executed task
	 * @param entry
	 */
	private void handleTaskResult(TaskResultMessage msg){
		FutureTaskResult<Object> future = tasksResults.get(msg.getTaskID());
		future.set(msg.getResult());
		e.eventTaskResult(msg.getTaskID());
	}
	
	
	/**
	 * Handle the TASK_ERROR message
	 * TASK_ERROR contains a throwable catch in the execution of the task  
	 */
	private void handleTaskError(TaskErrorMessage msg){
		FutureTaskResult<Object> future = tasksResults.get(msg.getTaskID());
		future.set(null);
		e.eventTaskError(msg.getTaskID(),msg.getThrowable());
	}
	
	
	/**
	 * Sends the response to the src of the message with the task.
	 * Used when a TaskRequest arrives
	 * @param id
	 * @param src
	 */
	private void taskResponse(TaskID id, Address src){
		if ( id== null)
			e.eventError("Null ID received in a Task Request");
		else{
			try {
				Task t = null;
				synchronized(tasksMap){
					for (TaskID tid : tasksMap.keySet())
						if (tid.equals(id)){
							t = tasksMap.get(tid);
							break;
						}							
//						t = tasksMap.get(id);
				}
				if (t != null){
					channel.send(src, new TaskResponseMessage(t,id));
					e.eventTaskResponse(id);
				}
				else
					e.eventError("Task request with Error, no task assigned to the ID " + id.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

//   DEPRECATED, now the superclass assigns null to handlers and the masterNode handleNodeCrash assign them
//	/**
//	 * Override from TasksNode. Master must assign a new slave the submitted tasks.
//	 */
//	@Override
//	protected void editTasks(Address address){
//		Vector<TaskEntry> tasks = new Vector<TaskEntry>();
//		synchronized(tasksIndex){
//			for(TaskEntry te : tasksIndex){
//				if (te.getOwner().equals(address)){
//					tasksIndex.remove(te);
//				}
//				if (te.getOwner().equals(info.getAddress()) && te.getHandler().equals(address) && te.getState()!= StateType.FINISHED){
//					tasks.add(te);
//				}
//			}
//		}
//		if (tasks.size() > 0)
//			schStrat.assign(tasks, nodesInfo);
//	}
	
}

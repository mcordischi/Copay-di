package node;


import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;


import message.*;
import message.TaskMessage.MessageType;
import node.NodeInformation.NodeType;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;

import event.Eventable;

import algorithm.SchedulerStrategy;

import task.*;

public class MasterNode extends TasksNode implements Master {
	

	private SchedulerStrategy schStrat;
	private Map<TaskID,FutureTaskResult> tasksResults = new ConcurrentHashMap<TaskID,FutureTaskResult>();
	private Map<TaskID,Task> tasksMap = new ConcurrentHashMap<TaskID,Task>();


	
	
// ******************************
//	METHODS
// ******************************
	
	public MasterNode(Eventable e,SchedulerStrategy schStrat){
		super(e);
		this.schStrat = schStrat;
		setLocalState(PAUSE);
		setFinished(true);
		nodeType = NodeType.MASTER;
	}


	public MasterNode(Eventable e){
		super(e);
		this.schStrat = null;
		setLocalState(PAUSE);
		setFinished(true);
		nodeType = NodeType.MASTER;
	}

	@Override
	public void viewAccepted(View view) {
		super.viewAccepted(view);
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
		schStrat.assign(entry, nodesInfo);
		
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
		tasksMap.remove(id);
		tasksResults.get(id).cancel(true);
		tasksResults.remove(id);
		
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
	 * @param tasks : A vector of the tasks to schedule
	 */
	private void scheduleTasks(Vector<TaskEntry> tasks){
		schStrat.assign(tasks,nodesInfo);
	}
	
	@Override
	public void receive(Message msg) {
		TaskMessage tmsg = (TaskMessage) msg.getObject();
		switch (tmsg.getType()){
		case TASK_RESULT :
				handleTaskResult(((TaskResultMessage)tmsg));
			break;
		case TASK_REQUEST :
			TaskID id = ((TaskRequestMessage)tmsg).getId();
			taskResponse(id,msg.getSrc());
			break;
		case TASK_ERROR :
			handleTaskError((TaskErrorMessage)tmsg);
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


}

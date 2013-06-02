package node;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


import message.*;

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


	public static boolean WORKING = true ;
	public static boolean PAUSE = false ;
	
	
// ******************************
//	METHODS
// ******************************
	
	public MasterNode(Eventable e,SchedulerStrategy schStrat){
		super(e);
		this.schStrat = schStrat;
		setLocalState(PAUSE);
		setFinished(true);
	}

	

	@Override
	public void viewAccepted(View arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public FutureTaskResult<Object> submit(Task t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void cancel(TaskID id) {
		// TODO Auto-generated method stub

	}


	
	@Override
	public void receive(Message msg) {
		// TODO Complete
		TaskMessage tmsg = (TaskMessage) msg.getObject();
		switch (tmsg.getType()){
		case TASK_RESULT :
				handleTaskResult(((TaskResult)tmsg));
			break;
		case TASK_REQUEST :
			TaskID id = ((TaskRequest)tmsg).getId();
			taskResponse(id,msg.getSrc());
			break;
		case TASK_ERROR :
			handleTaskError((TaskError)tmsg);
		default:
			super.receive(msg);
		}
	}
		
	
	/**
	 * Handle the TASK_RESULT message
	 * TASK_RESULT contains the result of the executed task
	 * @param entry
	 */
	private void handleTaskResult(TaskResult msg){
		//TODO Handle the Future. 
		//Notify nodes of the task completed? Or the Slave is the responsible?
	}
	
	
	/**
	 * Handle the TASK_ERROR message
	 * TASK_ERROR contains a throwable catch in the execution of the task  
	 */
	private void handleTaskError(TaskError msg){
		//TODO
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
					channel.send(src, new TaskResponse(t,id));
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

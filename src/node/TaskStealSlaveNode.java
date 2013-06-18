package node;

import message.TaskMessage;
import message.TaskNotificationMessage;

import org.jgroups.Address;

import task.TaskEntry;
import algorithm.TaskStealingStrategy;
import event.Eventable;


/**
 * Simple Slave, with Stealing at task level. When the slave has no more tasks, it simply steals and execute with no request.
 * There could be some synchronization problems, specially when 2 Slaves steal the same Task at the same time. Now you are warned.
 * 
 * CAUTION: need maintenance.
 * Last changes not tested
 * @author marto
 *
 */
public class TaskStealSlaveNode extends SlaveNode {

	TaskStealingStrategy stlStrat;
	
	
	public TaskStealSlaveNode(Eventable e, TaskStealingStrategy stlStrat ,int maxThreads) {
		super(e, maxThreads);
		this.stlStrat = stlStrat;
	}

	public void run() {
		while(!destroyFlag){
			//Waiting
				while (pendingTasks.size() >= 2*maxThreads )
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e.eventError("Thread.sleep\n" + e1.getCause());
					}
				while( getSystemState() != WORKING || getLocalState()!= WORKING )
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {
						e.eventError("Thread.sleep\n" + e1.getCause());
					}
			//finished
				int time = 2000;
				while(time<50000 && isFinished()){
					try{
						Thread.sleep(time);
					}catch (InterruptedException e1) {
						e.eventError("Thread.sleep\n" + e1.getCause());
					}
					time *= 1.2;
				}
				e.eventWarning("Time to fetch");
			//Fetch or Steal & Request
			TaskEntry entry = fetchTask();
			if(entry != null){
				setFinished(false);
				requestTask(entry);
			}
			else {
				setFinished(true);
			}
		}
	}

	
	/**
	 * Looks for Available tasks. If there is no task for the node, it take a null task or
	 * calls a stealing algorithm to steal from other node.
	 *
	 */
	protected synchronized TaskEntry fetchTask(){
		Address localAddress = channel.getAddress();
		for (TaskEntry entry : tasksIndex){
			if (entry.getHandler().equals(localAddress) && entry.getState().equals(TaskEntry.StateType.SUBMITTED))
				return entry;
		}
		TaskEntry result = stlStrat.steal(tasksIndex);
			if (result == null)
				return null;
		synchronized(result){
			result.setHandler(channel.getAddress());
		}
		try {
			channel.send(null, new TaskNotificationMessage(TaskMessage.MessageType.TASK_STEAL,result));
		} catch (Exception e1) {
			e.eventError("Steal message failed. Are you still connected to the cluster?");
			return null;
		}
		return result ;
	}
}

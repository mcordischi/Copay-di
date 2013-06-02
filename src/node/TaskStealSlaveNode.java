package node;

import message.TaskMessage;
import message.TaskNotificationMessage;

import org.jgroups.Address;

import task.TaskEntry;
import algorithm.StealingStrategy;
import event.Eventable;

public class TaskStealSlaveNode extends SlaveNode {

	StealingStrategy stlStrat;
	
	
	public TaskStealSlaveNode(Eventable e, StealingStrategy stlStrat ,int maxThreads) {
		super(e, maxThreads);
		this.stlStrat = stlStrat;
	}

	@Override
	protected void start() {
		while (getGlobalState() == WORKING && getLocalState()== WORKING && !isFinished()){
			TaskEntry entry = fetchTask();
			if(entry != null){
				while(pendingTasks.size() >= maxThreads)
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e.eventError("Thread.sleep\n" + e1.getCause());
					}
				requestTask(entry);
			}
			else {
				e.eventLocalCompletion();
				setFinished(true);
			}
		}
	}

	
	/**
	 * Looks for Available tasks. If there is no task for the node, it take a null task or
	 * calls a stealing algorithm to steal from other node.
	 *
	 */
	public synchronized TaskEntry fetchTask(){
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

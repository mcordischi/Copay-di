package node;

import message.NodeStealRequestMessage;
import message.TaskMessage;
import message.TaskNotificationMessage;

import org.jgroups.Address;

import task.TaskEntry;
import algorithm.NodeStealingStrategy;
import event.Eventable;

public class NodeStealSlaveNode extends SlaveNode {

	protected NodeStealingStrategy stlStrat;
	
	public NodeStealSlaveNode(Eventable e, NodeStealingStrategy stlStrat, int maxThreads) {
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
				requestSteal();
				e.eventLocalCompletion();
				setFinished(true);
			}
		}
	}
	
	
	protected void requestSteal(){
		Address victim = stlStrat.steal(nodesInfo, info);
		if (victim != null)
			try {
				channel.send(victim, new NodeStealRequestMessage(info.getWorkingCapacity() - info.getWorkingTasks()));
				e.eventNodeStealRequest(victim, info.getAddress());
			} catch (Exception e1) {
				e.eventError("Steal message failed. Are you still connected to the cluster?");
	
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
		return null ;
	}
	
}

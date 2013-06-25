package node;

import message.NodeStealRequestMessage;
import message.TaskMessage;
import message.TaskNotificationMessage;

import org.jgroups.Address;

import task.TaskEntry;
import algorithm.NodeStealingStrategy;
import event.Eventable;


/**
 * Just a common Slave, with stealing at Node level. When the Slave has no more tasks, it sends a request to a victim 
 * chosen with a {@link NodeStealingStrategy} given.
 *  
 * @author marto
 *
 */
public class NodeStealSlaveNode extends SlaveNode {

	protected NodeStealingStrategy stlStrat;
	
	public NodeStealSlaveNode(Eventable e, NodeStealingStrategy stlStrat, int maxThreads) {
		super(e, maxThreads);
		this.stlStrat = stlStrat;
	}

	/**
	 * Requests 2 times the pool size
	 */
	@Override
	public void run() {
//		if (!finishedLock.tryLock()){
//			finishedLock.lock();
//			flagLock.lock();
//			setFlag(true);
//			flagLock.unlock();
//		}
//		e.eventWarning("Starting RUN in new thread");
		while(!destroyFlag){
//		while (getGlobalState() == WORKING && getLocalState()== WORKING && (!isFinished() || isFlag())){
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
			//Fetch & Request
			TaskEntry entry = fetchTask();
			if(entry != null){
				setFinished(false);
				requestTask(entry);
			}
			else {
				//Steal
				if (stlStrat != null)
					requestSteal();
				setFinished(true);
			}
		}
//		finishedLock.unlock();
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
	protected TaskEntry fetchTask(){
		Address localAddress = channel.getAddress();
		TaskEntry result = null;
		synchronized(tasksIndex){
			try {
				tasksIndexSem.acquire();
			} catch (InterruptedException e1) {
				e.eventError("Internal Erorr - Semaphores");
			}
			for (TaskEntry entry : tasksIndex){
				if (entry.getHandler() != null)
					if (entry.getHandler().equals(localAddress) && entry.getState().equals(TaskEntry.StateType.SUBMITTED))
						result = entry;
			}
			tasksIndexSem.release();
		}
		return result ;
	}
	
}

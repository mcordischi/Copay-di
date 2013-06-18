package node;
import task.*;

/**
 * Interface used for easy usage of the {@link MasterNode}.
 * @author marto
 *
 */
public interface Master extends Node{
	
	/**
	 * Submits a task to the cluster, returns a Future.
	 * @param t The task
	 * @return the future
	 */
	public abstract FutureTaskResult<Object> submit(Task t);
	
	
	/**
	 * Sends a REMOVE_TASK signal to the cluster 
	 * @param id
	 */
	public abstract void cancel(TaskID id);

	

}

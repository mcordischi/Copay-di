package node;
import task.*;


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

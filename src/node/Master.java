package node;
import task.*;


public interface Master extends Node{
	
	/**
	 * Submits a task to the cluster, returns the "Ticket" to the answer.
	 * @param t
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public abstract FutureTaskResult<Object> submit(Task t);
	
	public abstract void cancel(TaskID id);

}

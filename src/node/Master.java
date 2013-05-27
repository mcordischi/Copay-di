package node;
import task.*;


public interface Master {
	
	/**
	 * Submits a task to the cluster, returns the "Ticket" to the answer.
	 * @param t
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public abstract FutureTaskResult<Object> submit(Task t);
	
	public abstract void cancel(Task t);
	
	public abstract void connect(String Cluster);
	
//	Used in TaskResult
//	public boolean isDone(Task id);
//	
//	public Object getResult(TaskID id);
	
	public void setGlobalState(boolean GlobalState);
}

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
	public abstract TaskID submit(Task t, long timeout) throws Exception;
	
	public abstract void connect(String Cluster) throws Exception;
	
	public boolean isDone(TaskID id);
	
	public Object getResult(TaskID id);
	
	public void setGlobalState(boolean GlobalState) throws Exception;
}

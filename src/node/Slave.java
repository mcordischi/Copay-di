package node;
import task.Task;
import task.TaskEntry;
import task.TaskID;


public interface Slave {
	public abstract void handle(TaskID id, Task task) throws Exception;
	public abstract void connect(String Cluster) throws Exception;
}

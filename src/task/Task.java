package task;
import java.io.Serializable;

/**
 * Interface for tasks. The only method (execute()) returns an object.
 * 
 */
public interface Task extends Serializable,Runnable{
	public abstract Object execute();
}
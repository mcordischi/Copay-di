package task;
import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * Interface for tasks. The only method (execute()) returns an object.
 * 
 */
public interface Task extends Serializable,Callable<Object>{
	public abstract Object call();
}
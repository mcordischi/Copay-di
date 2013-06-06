package task;
import java.io.Serializable;
import java.util.concurrent.Callable;
import java.lang.Throwable;

/**
 * Interface for tasks. The only method (execute()) returns an object.
 * 
 */
public interface Task extends Serializable,Callable<Object>{
	public abstract Object call() throws Exception;
}
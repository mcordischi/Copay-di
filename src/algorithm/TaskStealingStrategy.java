package algorithm;

import java.util.Collection;

import task.TaskEntry;

/**
 * Returns a task from the full list of tasks. Be careful when implementing it, there may be some finished tasks. 
 * @author marto
 *
 */
public interface TaskStealingStrategy {
		public abstract TaskEntry steal(Collection<TaskEntry> tasks);
}

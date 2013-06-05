package algorithm;

import java.util.Collection;

import task.TaskEntry;


public interface TaskStealingStrategy {
		public abstract TaskEntry steal(Collection<TaskEntry> tasks);
}

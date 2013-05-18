package algorithm;

import java.util.Collection;

import task.TaskEntry;


public interface StealingStrategy {
		public abstract TaskEntry steal(Collection<TaskEntry> tasks);
}

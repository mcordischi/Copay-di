package algorithm;

import java.util.Collection;

import task.TaskEntry;

/**
 * No steals, not today.
 * @author marto
 *
 */
public class NullStealingStrategy implements TaskStealingStrategy {

	@Override
	public TaskEntry steal(Collection<TaskEntry> tasks) {
		for(TaskEntry t: tasks)
				if(t.getHandler() == null){
					return t;
				}
		return null;
	}

}

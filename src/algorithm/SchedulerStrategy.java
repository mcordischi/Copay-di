package algorithm;

import java.util.Collection;

import node.NodeInformation;

import org.jgroups.View;

import task.TaskEntry;

/**
 * Schedules the Handler for new tasks. Used by {@link Master} when a new task is submitted.
 * @author marto
 *
 */
public interface SchedulerStrategy {
	public abstract void assign(TaskEntry task, Collection<NodeInformation> nodesInfo);
	public abstract void assign(Collection<TaskEntry> tasks, Collection<NodeInformation> nodesInfo);

}

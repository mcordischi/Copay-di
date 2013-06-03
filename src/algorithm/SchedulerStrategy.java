package algorithm;

import java.util.Collection;

import node.NodeInformation;

import org.jgroups.View;

import task.TaskEntry;


public interface SchedulerStrategy {
	public abstract void assign(TaskEntry task, Collection<NodeInformation> nodesInfo);
	public abstract void assign(Collection<TaskEntry> tasks, Collection<NodeInformation> nodesInfo);

}

package node;

import java.util.Collection;

import org.jgroups.Address;
import org.jgroups.View;

import task.TaskEntry;

/**
 * Interface used for monitoring the cluster, nodes and tasks.
 * 
 * Implementing class: {@link TasksNode}
 * @author marto
 *
 */
public interface Monitor extends Node {
		public View getClusterInfo();
		
		public Collection<TaskEntry> getTasksInfo();
		
		public Collection<NodeInformation> getNodeInfo();
}

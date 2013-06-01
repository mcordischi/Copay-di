package node;

import java.util.Collection;

import org.jgroups.Address;

import task.TaskEntry;

public interface Controller extends Node {
		public Collection<Address> getClusterInfo();
		
		public Collection<TaskEntry> getTasksInfo();
		
		public Collection<NodeInformation> getNodeInfo();
}

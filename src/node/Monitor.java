package node;

import java.util.Collection;

import org.jgroups.Address;

import task.TaskEntry;

public interface Monitor extends Node {
		public Collection<Address> getClusterInfo();
		
		public Collection<TaskEntry> getTasksInfo();
		
		public Collection<NodeInformation> getNodeInfo();
}

package algorithm;

import java.util.Collection;

import org.jgroups.Address;

import message.NodeStealRequestMessage;
import node.NodeInformation;

import task.TaskEntry;

public interface NodeStealingStrategy {
	
	/**
	 * TODO ask for a specific number of tasks
	 * @param nodesInfo
	 * @param localInfo
	 * @return The node to request a steal.
	 */
	public abstract Address steal(Collection<NodeInformation> nodesInfo, NodeInformation localInfo);

}

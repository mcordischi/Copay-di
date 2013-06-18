package algorithm;

import java.util.Collection;

import org.jgroups.Address;

import message.NodeStealRequestMessage;
import node.NodeInformation;
import node.NodeStealSlaveNode;

import task.TaskEntry;


/**
 * Used by {@link NodeStealSlaveNode} to select a victim to steal tasks from all the nodes of the cluster. 
 * 
 * Be careful when implementing, choose a SLAVE!
 * 
 * @author marto
 *
 */
public interface NodeStealingStrategy {
	
	/**
	 * @param nodesInfo
	 * @param localInfo
	 * @return The node to request a steal.
	 */
	public abstract Address steal(Collection<NodeInformation> nodesInfo, NodeInformation localInfo);

}

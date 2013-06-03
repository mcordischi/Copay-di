package node;

import java.io.Serializable;

import message.NodeInfoMessage;
import message.TaskMessage;

import org.jgroups.Address;
import org.jgroups.Channel;

/**
 * Interface that controls the Node Information. 
 * Every node must send information about its state to the cluster. Every node must have a collection
 * of NodeInformation, representing the state of all the nodes in the cluster.
 * Used to improve the scheduling and stealing algorithms.
 * @author marto
 *
 */
public interface NodeInformation extends Serializable{
		/**	
		 * Gets the address of the Node
		 * @return The address
		 */
		public abstract Address getAddress();
		
		/**
		 * @return The local working state
		 */
		public abstract boolean getWorkingState();
		
		
		public static enum NodeType {MASTER,SLAVE,MONITOR};
		
		/**
		 *	returns which type of node it is 
		 */
		public abstract NodeType getNodeType();
		
		/**
		 * Get the number of tasks that the node is processing
		 * @return
		 */
		public abstract int	getWorkingTasks();
		
		
		/**
		 * Send a request to the node to update the information
		 */
		public abstract void forceUpdate(Channel c) throws Exception;
		
		/**
		 * Updates the information
		 */
		public abstract void update(NodeInformation update);
}

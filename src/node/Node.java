package node;

import org.jgroups.Receiver;

public interface Node extends Receiver {
	
		/**
		 * Connects to the cluster.
		 * @param cluster : The cluster name
		 */
		public void connect(String cluster);
		
		/**
		 * Returns true if the cluster is working. False if it has finished or 
		 * it is on pause.
		 */
		public boolean getGlobalState();

		
		public static boolean WORKING = true ;
		public static boolean PAUSE = false ;
}

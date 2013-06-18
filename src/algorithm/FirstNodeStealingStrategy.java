package algorithm;

import java.util.Collection;

import org.jgroups.Address;

import message.NodeStealRequestMessage;
import node.NodeInformation;

/**
 * 
 * @author marto
 *
 */
public class FirstNodeStealingStrategy implements NodeStealingStrategy {

	@Override
	public Address steal(Collection<NodeInformation> nodesInfo,
			NodeInformation localInfo) {
		for( NodeInformation i : nodesInfo){
			if ((! i.equals(localInfo)) && (i.getWorkingCapacity() > 0))
				return i.getAddress();
		}
	return null;
	}
	
}

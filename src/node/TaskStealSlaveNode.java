package node;

import algorithm.StealingStrategy;
import event.Eventable;

public class TaskStealSlaveNode extends SlaveNode {

	StealingStrategy stlStrat;
	
	
	public TaskStealSlaveNode(Eventable e, StealingStrategy stlStrat ,int maxThreads) {
		super(e, maxThreads);
		this.stlStrat = stlStrat;
	}

	@Override
	protected void start() {
		// TODO Auto-generated method stub

	}

}

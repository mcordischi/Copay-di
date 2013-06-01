package node;

import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import event.Eventable;

import task.TaskEntry;

import algorithm.StealingStrategy;

public class SlaveNode extends TasksNode implements Slave {

	private final ExecutorService executor;
	private int maxThreads;
	
	private StealingStrategy stlStrat;
	
	private Vector<TaskEntry> pendingTasks = new Vector<TaskEntry>();
	private Map<TaskEntry,Future<Object> > workingTasks = new ConcurrentHashMap<TaskEntry, Future<Object> >();
	
	
	public SlaveNode(Eventable e, StealingStrategy stlStrat, int maxThreads) {
		super(e);
		this.stlStrat = stlStrat;
		this.maxThreads = maxThreads;
		this.executor = new ScheduledThreadPoolExecutor(maxThreads);
	}
	
	@Override
	public void setLocalState(boolean localState) {
		// TODO Auto-generated method stub

	}
	
	
	private void start(){
		//TODO
	}


}

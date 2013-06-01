package node;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.View;

import event.Eventable;

import algorithm.SchedulerStrategy;

import task.FutureTaskResult;
import task.Task;
import task.TaskID;

public class MasterNode extends TasksNode implements Master {
	

	private SchedulerStrategy schStrat;
	
	private Map<TaskID,FutureTaskResult> tasksResults = new ConcurrentHashMap<TaskID,FutureTaskResult>();
	
	private Map<TaskID,Task> tasksMap = new ConcurrentHashMap<TaskID,Task>();


	public static boolean WORKING = true ;
	public static boolean PAUSE = false ;
	
	
// ******************************
//	METHODS
// ******************************
	
	public MasterNode(Eventable e,SchedulerStrategy schStrat){
		super(e);
		this.schStrat = schStrat;
		this.localState = PAUSE;
		this.finished = true;
	}
	
	
	
	@Override
	public void receive(Message arg0) {
		// TODO Auto-generated method stub

	}


	@Override
	public void viewAccepted(View arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public FutureTaskResult<Object> submit(Task t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void cancel(TaskID id) {
		// TODO Auto-generated method stub

	}



	
}

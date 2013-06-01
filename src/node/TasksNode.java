package node;

import java.util.Vector;

import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;

import event.Eventable;

import task.TaskEntry;

public class TasksNode extends ReceiverAdapter implements Node {
	
	private Eventable e;

	protected Vector<TaskEntry> tasksIndex = new Vector<TaskEntry>();
	
	protected JChannel channel;
	protected View actualView;
	
	protected boolean globalState;
	protected boolean localState;
	protected boolean finished;
	
	public static boolean WORKING = true ;
	public static boolean PAUSE = false ;
	
	
	public TasksNode(Eventable e){
		this.e = e;
	}
	
	@Override
	public void connect(String cluster) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean getGlobalState() {
		return globalState;	
	}
	
	
	@Override
	public void receive(Message msg){
		//TODO
	}

}

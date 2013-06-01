package task;
import java.io.Serializable;

import org.jgroups.Address;
import org.jgroups.util.Streamable;


public class TaskEntry implements Serializable{
	private TaskID id;
	private Address handler;
//	private Object result;
//	private long timeout;
	public static enum StateType{WORKING,SUBMITTED,FINISHED,TIMEOUT,UNDEFINED};
	private StateType state;
	
	public TaskEntry (TaskID id, Address handler){
		this.id = id;
		this.handler = handler;
		state = StateType.SUBMITTED;
	}

	@Override
	public boolean equals(Object other){
		if (id.equals(((TaskEntry)other).getId()))
			return true;
		return false;
		
	}
	
	@Override
	public int hashCode(){
		return id.hashCode();
	}
	
	public String toString(){
		String str = "TE:" + id.toString() + "\tHandler: " + handler.toString() + "\tState: " + state ; 
		return str;
	}
	
	public Address getOwner(){
		return id.getOwner();
	}


	public TaskID getId() {
		return id;
	}


	public void setId(TaskID id) {
		this.id = id;
	}


	public Address getHandler() {
		return handler;
	}


	public void setHandler(Address handler) {
		this.handler = handler;
	}


	public StateType getState() {
		return state;
	}


	public void setState(StateType state) {
		this.state = state;
	}


	
	

}

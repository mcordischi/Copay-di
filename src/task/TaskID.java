package task;
import java.io.Serializable;

import org.jgroups.Address;


public class TaskID implements Serializable {
	
	private Address owner;
	private int id;
	private static int next_id=1;
	
	public TaskID(Address owner,int id){
		this.owner = owner;
		this.id = id;				
	}
	
		
	public static synchronized TaskID newTask(Address owner){
		return new TaskID(owner,next_id++);
	}
	
	@Override
	public boolean equals(Object other){
		TaskID oid = (TaskID) other ;
		if (owner.equals(oid.getOwner()) && id == oid.getId())
			return true;
		return false;					
	}
	
	public String toString(){
		return id + ":" + owner.toString();
	}

	public Address getOwner() {
		return owner;
	}

	public void setOwner(Address owner) {
		this.owner = owner;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	

}

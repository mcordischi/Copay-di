package node;

import message.NodeInfoMessage;
import message.TaskMessage;
import message.TaskMessage.MessageType;

import org.jgroups.Address;
import org.jgroups.Channel;

import task.TaskID;

//TODO implement automatic Updates

public class TasksNodeInformation implements NodeInformation {

	private Address address;
	private NodeType nodeType;
	private boolean localWorkingState;
	private int tasksHandling;
	private int nodeCapacity;
	
	
	
	public TasksNodeInformation(Address address, NodeType nodeType, boolean localState, int nodeCapacity){
		this.address = address;
		this.nodeType = nodeType;
		this.localWorkingState  = localState;
		tasksHandling = 0 ;
		this.nodeCapacity = nodeCapacity;
	}
	
	@Override
	public Address getAddress() {
		return address;
	}

	@Override
	public boolean getWorkingState() {
		return localWorkingState;
	}

	@Override
	public NodeType getNodeType() {
		return nodeType;
	}

	@Override
	public int getWorkingTasks() {
		return tasksHandling;
	}

	@Override
	public void forceUpdate(Channel c) throws Exception{
		//if the update is made by the owner of this information
		if (c.getAddress().equals(address))
			c.send(null, new NodeInfoMessage(this));
		else
		//Send an update request
			c.send(address,new TaskMessage(MessageType.INFORMATION_REQUEST));
	}

	@Override
	public void update(NodeInformation update) {
		this.localWorkingState = update.getWorkingState();
		this.tasksHandling = update.getWorkingTasks();
	}

	@Override
	public boolean equals(Object other){
		NodeInformation o = (NodeInformation)other;
		if ( this.address.equals(o.getAddress()))
			return true;
		return false;
	}
	
	public boolean equals(Address address){
		if (address.equals(this.address))
			return true;
		return false;
	}
	

	@Override
	public int getWorkingCapacity() {
		return nodeCapacity;
	}

	@Override
	public void addTask() {
		tasksHandling++;		
	}

	@Override
	public void removeTask() {
		tasksHandling--;		
	}

	@Override
	public void changeState(boolean newState) {
		localWorkingState = newState;
	}
}

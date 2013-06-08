package event;

import node.NodeInformation;

import org.jgroups.Address;

import task.TaskEntry;
import task.TaskID;

public class DebugInterface implements Eventable {

	private String prefix;
	
	public DebugInterface(String prefix){
		this.prefix = prefix;
	}
	
	@Override
	public void eventNewTask(TaskEntry entry) {
//		System.out.println(prefix + ": New Task\t"+ entry.toString());
	}

	@Override
	public void eventRemoveTask(TaskID id) {
		System.out.println(prefix + ": Task removed\t"+ id.getId() + ":" + id.getOwner().toString());

	}

	@Override
	public void eventTaskComplete(TaskEntry entry) {
//		TaskID id = entry.getId();
//		System.out.println(prefix + ": Task completed\t"+ id.getId() + ":" + id.getOwner().toString());

	}

	@Override
	public void eventTaskExecution(TaskEntry entry) {
		TaskID id = entry.getId();
		System.out.println(prefix + ": Task Execution\t"+ id.getId() + ":" + id.getOwner().toString());

	}

	@Override
	public void eventTaskSteal(TaskEntry entry) {
		TaskID id = entry.getId();
		System.out.println(prefix + ": Task Steal\t"+ id.getId() + ":" + id.getOwner().toString());

	}
	

	@Override
	public void eventTaskUpdate(TaskEntry entry) {
//		TaskID id = entry.getId();
//		System.out.println(prefix + ": Task updated\t"+ id.getId() + ":" + id.getOwner().toString());

	}

	@Override
	public void eventTaskRequest(TaskID id) {
		System.out.println(prefix + ": Task request\t"+ id.getId() + ":" + id.getOwner().toString());

	}

	@Override
	public void eventTaskResponse(TaskID id) {
		System.out.println(prefix + ": Task Response\t"+ id.getId() + ":" + id.getOwner().toString());

	}

	@Override
	public void eventSystemState(boolean state) {
		String m;
		if (state)
			m = "RESUME";
		else
			m = "PAUSE";
		System.out.println(prefix + ": ***\tSYSTEM "+ m +"\t***");

	}

	@Override
	public void eventLocalState(boolean state) {
		String m;
		if (state)
			m = "resume";
		else
			m = "pause";
		System.out.println(prefix + ": ---\tLocal "+ m +"\t---");

	}

	@Override
	public void eventNodeCrash(Address node) {
		System.out.println(prefix + ": Node Crashed\t" + node.toString());

	}

	@Override
	public void eventNodeAvailable(Address node) {
		System.out.println(prefix + ": New Node\t" + node.toString());

	}

	@Override
	public void eventWarning(String str) {
		System.out.println(prefix + ": WARNING - " + str);		
	}

	@Override
	public void eventError(String str) {
		System.out.println(prefix + ": ERROR - " + str);
		
	}

	@Override
	public void eventLocalCompletion() {
		System.out.println(prefix + ": The node finished its jobs");		
	}


	@Override
	public void eventTaskResult(TaskID id) {
		System.out.println(prefix + ": Task Result Received: \t" + id.toString() );
		
	}

	@Override
	public void eventInformation(NodeInformation i) {
		System.out.print(prefix + ": NODE INFO: " + i.getAddress().toString() + " is a " + i.getNodeType() 
				+ " and is ");
		if (i.getWorkingState())
			System.out.println("working with " + i.getWorkingTasks() + "/" + i.getWorkingCapacity() + " tasks.");
		else
			System.out.println("NOT working.");
	}

	@Override
	public void eventNodeStealRequest(Address victim, Address stealer) {
		System.out.println(prefix + ": Steal Request sent to " + victim.toString());		
	}

	@Override
	public void eventNodeStealResponse(boolean state, Address victim,
			Address stealer) {
		if (state == false)
			System.out.println(prefix + ": Steal from " + stealer + " DENIED.");
		else
			System.out.println(prefix + ": Steal ACCEPTED from " + stealer);
		
	}

	@Override
	public void eventTaskError(TaskID id, Throwable t) {
		System.out.println(prefix +": Exception received from_" + id.toString() + "\n\t\t" + t.getMessage());		
	}

	@Override
	public void notifyTask(TaskEntry entry) {
		System.out.println(prefix + "-TaskInfo:\t"+ entry.toString());
	}

}

package event;

import node.NodeInformation;

import org.jgroups.Address;

import task.TaskEntry;
import task.TaskID;

public class DebugInterface implements Eventable {

	@Override
	public void eventNewTask(TaskEntry entry) {
		System.out.println("New Task\t"+ entry.toString());
	}

	@Override
	public void eventRemoveTask(TaskID id) {
		System.out.println("Task removed\t"+ id.getId() + ":" + id.getOwner().toString());

	}

	@Override
	public void eventTaskComplete(TaskEntry entry) {
		TaskID id = entry.getId();
		System.out.println("Task completed\t"+ id.getId() + ":" + id.getOwner().toString());

	}

	@Override
	public void eventTaskExecution(TaskEntry entry) {
		TaskID id = entry.getId();
		System.out.println("Task Execution\t"+ id.getId() + ":" + id.getOwner().toString());

	}

	@Override
	public void eventTaskSteal(TaskEntry entry) {
		TaskID id = entry.getId();
		System.out.println("Task Steal\t"+ id.getId() + ":" + id.getOwner().toString());

	}

	@Override
	public void eventTaskUpdate(TaskEntry entry) {
		TaskID id = entry.getId();
		System.out.println("Task updated\t"+ id.getId() + ":" + id.getOwner().toString());

	}

	@Override
	public void eventTaskRequest(TaskID id) {
		System.out.println("Task request\t"+ id.getId() + ":" + id.getOwner().toString());

	}

	@Override
	public void eventTaskResponse(TaskID id) {
		System.out.println("Task Response\t"+ id.getId() + ":" + id.getOwner().toString());

	}

	@Override
	public void eventSystemPause() {
		System.out.println("***\tSYSTEM PAUSE\t***");

	}

	@Override
	public void eventSystemResume() {
		System.out.println("***\tSYSTEM RESUME\t***");

	}

	@Override
	public void eventLocalPause() {
		System.out.println("---\tLocal Pause\t---");

	}

	@Override
	public void eventNodeCrash(Address node) {
		System.out.println("Node Crashed\t" + node.toString());

	}

	@Override
	public void eventNodeAvailable(Address node) {
		System.out.println("New Node\t" + node.toString());

	}

	@Override
	public void eventWarning(String str) {
		System.out.println("WARNING: " + str);		
	}

	@Override
	public void eventError(String str) {
		System.out.println("ERROR: " + str);
		
	}

	@Override
	public void eventLocalCompletion() {
		System.out.println("The node finished its jobs");		
	}


	@Override
	public void eventTaskResult(TaskID id) {
		System.out.println("Task Result Received: \t" + id.toString() );
		
	}

	@Override
	public void eventInformation(NodeInformation i) {
		System.out.print("NODE INFO: " + i.getAddress().toString() + " is a " + i.getNodeType() 
				+ " and is ");
		if (i.getWorkingState())
			System.out.println("working with " + i.getWorkingTasks() + " tasks.");
		else
			System.out.println("NOT working.");
	}

}

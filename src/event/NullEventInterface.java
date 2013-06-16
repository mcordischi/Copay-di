package event;

import node.NodeInformation;

import org.jgroups.Address;

import task.TaskEntry;
import task.TaskID;

public class NullEventInterface implements Eventable {

	@Override
	public void eventNewTask(TaskEntry entry) {}

	@Override
	public void eventRemoveTask(TaskID id) {}

	@Override
	public void eventTaskComplete(TaskEntry entry) {}

	@Override
	public void eventTaskResult(TaskID id) {}

	@Override
	public void eventTaskExecution(TaskEntry entry) {}

	@Override
	public void eventTaskSteal(TaskEntry entry) {}

	@Override
	public void eventTaskUpdate(TaskEntry entry) {}

	@Override
	public void eventTaskRequest(TaskID id) {}

	@Override
	public void eventTaskResponse(TaskID id) {}

	@Override
	public void eventTaskError(TaskID id, Throwable t) {}

	@Override
	public void eventSystemState(boolean state) {}

	@Override
	public void eventLocalState(boolean state) {}

	@Override
	public void eventLocalCompletion() {}

	@Override
	public void eventNodeStealRequest(Address victim, Address stealer) {}

	@Override
	public void eventNodeStealResponse(boolean state, Address victim,
			Address stealer) {}

	@Override
	public void eventNodeCrash(Address node) {}

	@Override
	public void eventNodeAvailable(Address node) {}

	@Override
	public void eventWarning(String str) {}

	@Override
	public void eventError(String str) {}

	@Override
	public void eventInformation(NodeInformation i) {}

	@Override
	public void notifyTask(TaskEntry entry) {}

}

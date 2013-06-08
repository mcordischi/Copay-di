package event;

import node.NodeInformation;

import org.jgroups.Address;

import task.*;

/**
 * Catches the events that can be showed by the user interface
 * @author marto
 *
 */
public interface Eventable {
		public abstract void eventNewTask(TaskEntry entry);
		
		public abstract void eventRemoveTask(TaskID id);
		
		public abstract void eventTaskComplete(TaskEntry entry);
		
		public abstract void eventTaskResult(TaskID id);
		
		public abstract void eventTaskExecution(TaskEntry entry);
		
		public abstract void eventTaskSteal(TaskEntry entry);
		
		public abstract void eventTaskUpdate(TaskEntry entry);
		
		public abstract void eventTaskRequest(TaskID id);
		
		public abstract void eventTaskResponse(TaskID id);
		
		public abstract void eventTaskError(TaskID id, Throwable t);
		
		public abstract void eventSystemState(boolean state);
		
		public abstract void eventLocalState(boolean state);
		
		public abstract void eventLocalCompletion();
		
		public abstract void eventNodeStealRequest(Address victim, Address stealer);
		
		public abstract void eventNodeStealResponse(boolean state, Address victim, Address stealer);
		
		public abstract void eventNodeCrash(Address node);
		
		public abstract void eventNodeAvailable(Address node);
		
		public abstract void eventWarning(String str);
		
		public abstract void eventError(String str);
		
		public abstract void eventInformation(NodeInformation i);
		
		public abstract void notifyTask(TaskEntry entry);
}

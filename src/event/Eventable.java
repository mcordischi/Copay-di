package event;

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
		
		public abstract void eventTaskExecution(TaskEntry entry);
		
		public abstract void eventTaskSteal(TaskEntry entry);
		
		public abstract void eventTaskUpdate(TaskEntry entry);
		
		public abstract void eventTaskRequest(TaskID id);
		
		public abstract void eventTaskResponse(TaskID id);
		
		public abstract void eventSystemPause();
		
		public abstract void eventSystemResume();
		
		public abstract void eventLocalPause();
		
		public abstract void eventNodeCrash(Address node);
		
		public abstract void eventNodeAvailable(Address node);
		
		public abstract void eventWarning(String str);
		
		public abstract void eventError(String str);
}

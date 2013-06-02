package message;

import task.TaskEntry;

public class TaskNotificationMessage extends TaskMessage {

		TaskEntry entry;
		
		public TaskNotificationMessage(MessageType type, TaskEntry entry){
			super(type);
			this.entry = entry;
		}

		public TaskEntry getEntry() {
			return entry;
		}

		public void setEntry(TaskEntry entry) {
			this.entry = entry;
		}
		
		
}

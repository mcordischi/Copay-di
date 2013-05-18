package message;

import task.TaskEntry;

public class TaskNotification extends TaskMessage {

		TaskEntry entry;
		
		public TaskNotification(MessageType type, TaskEntry entry){
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

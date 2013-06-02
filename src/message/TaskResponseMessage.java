package message;
import task.Task;
import task.TaskEntry;
import task.TaskID;


public class TaskResponseMessage extends TaskRequestMessage {
		protected Task task;
		
		public TaskResponseMessage(Task task, TaskID id){
			super(TaskMessage.MessageType.TASK_RESPONSE,id);
			this.task = task;
		}

		public Task getTask() {
			return task;
		}

		public void setTask(Task task) {
			this.task = task;
		}
		
		
		
}

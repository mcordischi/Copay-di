package message;
import task.TaskEntry;
import task.TaskID;



public class TaskRequest extends TaskMessage{
		protected TaskID id;
		
		public TaskRequest(MessageType type, TaskID id){
			super(type);
			this.id = id;
			
		}

		public TaskID getId() {
			return id;
		}

		public void setId(TaskID id) {
			this.id = id;
		}


		
		
		
}

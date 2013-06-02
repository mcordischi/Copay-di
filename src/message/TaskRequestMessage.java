package message;
import task.TaskEntry;
import task.TaskID;



public class TaskRequestMessage extends TaskMessage{
		protected TaskID id;
		
		public TaskRequestMessage(MessageType type, TaskID id){
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

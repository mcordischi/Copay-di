package task;

public class InterruptableTask implements Task {

	private Object result;
	private Task task;
	private volatile boolean interrupted = false;
	
	public InterruptableTask(Task task){
		this.task=task;
	}
	
	public void interrupt(){
		this.interrupted = true;
	}
	
	
	@Override
	public Object call() throws Exception {
	       while(!Thread.currentThread().isInterrupted()){  
	            if(!interrupted){  
	                //Do work here
	            	result = task.call();
	            	return result;
	            }
	            else{  
	                //Has been suspended  
                    while(interrupted){  
                        synchronized(result){  
                            result.wait();  
                        }                           
                    }                       
	            }                           
	        }  
        return null;
	}


}

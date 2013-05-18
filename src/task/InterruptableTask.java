package task;

public class InterruptableTask implements Runnable {

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
	public void run() {
        while(!Thread.currentThread().isInterrupted()){  
            if(!interrupted){  
                //Do work here
            	result = task.call();
            }
            else{  
                //Has been suspended  
                try {                   
                    while(interrupted){  
                        synchronized(result){  
                            result.wait();  
                        }                           
                    }                       
                }  
                catch (InterruptedException e) {                    
                }             
            }                           
        }  
        System.out.println("Cancelled");    

	}


}

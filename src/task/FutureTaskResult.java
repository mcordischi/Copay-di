package task;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import node.Master;

public class FutureTaskResult<V> implements Future<V> {
	
	V result = null;
	boolean cancel = false;
	boolean done = false;
	Master master;
	TaskID id;
	
	public FutureTaskResult (TaskID id){
		master = null;
		this.id = id;
	}
	
	
	public FutureTaskResult (TaskID id, Master master){
		this.master = master;
		this.id = id;
	}
	
	
	@Override
	public boolean cancel(boolean arg0) {
		cancel = true;
		master.cancel(id);
		return cancel;
	}
	
	@Override
	public V get() throws InterruptedException, ExecutionException {
		// TODO Block if result not available
		return result;
	}

	@Override
	public V get(long arg0, TimeUnit arg1) throws InterruptedException,
			ExecutionException, TimeoutException {
		// TODO Auto-generated method stub
		return result;
	}

	@Override
	public boolean isCancelled() {
		return cancel;
	}

	@Override
	public boolean isDone() {
		return done;
	}
	
	
	public void set(V result){
		this.result = result;
		done = true;
		//TODO notify
	}

	
	//GETTERS & SETTERS
	
	public TaskID getTaskID(){
		return id;
	}


	
	
}

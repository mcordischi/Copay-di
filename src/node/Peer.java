package node;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import message.*;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.util.Util;

import event.Eventable;

import task.*;

import algorithm.SchedulerStrategy;
import algorithm.StealingStrategy;


//TODO Implement Timeout

public class Peer extends ReceiverAdapter implements Master, Slave {
	
	final ExecutorService executor;
	private int maxThreads;
	private Vector<TaskEntry> tasksIndex = new Vector<TaskEntry>();
	private JChannel channel;
	private SchedulerStrategy schStrat;
	private StealingStrategy stlStrat;
	private Map<TaskID,Task> tasksMap = new HashMap<TaskID,Task>();
	private View actualView;
	private Eventable e;
	private boolean globalState;
	private boolean localState;
	private boolean finished;
	private Vector<TaskEntry> pendingTasks = new Vector<TaskEntry>();
	private Map<TaskEntry,Future<Object> > workingTasks = new HashMap<TaskEntry, Future<Object> >();
	public static boolean WORKING = true ;
	public static boolean PAUSE = false ;
	
	
	public Peer(Eventable e,SchedulerStrategy schStrat, StealingStrategy stlStrat, int maxThreads){
		this.e = e ;
		this.schStrat = schStrat;
		this.stlStrat = stlStrat;
		this.localState = PAUSE;
		this.finished = false;
		this.maxThreads = maxThreads;
		this.executor = new ScheduledThreadPoolExecutor(maxThreads);
	}

	public void connect(String cluster){
		try {
			channel = new JChannel();
			channel.setReceiver(this);
	        channel.connect(cluster);
	        channel.getState(null, 10000);
		} catch (Exception e1) {
			e.eventError("Could not connect to cluster");
		}
	}
	
	public void disconnect(){
		channel.close();
	}
	
	/**
	 * Working loop. It searches for tasks and execute them
	 */
	private synchronized void start() {
		while (globalState == WORKING && localState== WORKING && !finished){
			TaskEntry entry = fetchTask();
			if(entry != null){
				while(pendingTasks.size() >= maxThreads)
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e.eventError("Thread.sleep\n" + e1.getCause());
					}
				requestTask(entry);
			}
			else {
				e.eventLocalCompletion();
				finished = true;
			}
		}
	}
	
	
	/**
	 * Sends a request to the owner of the task, asking for the task. The answer is handle in {@link #receive(Message)}.
	 * @param entry
	 * @param task
	 */
	public void requestTask(TaskEntry entry){
		synchronized(entry){
			entry.setState(TaskEntry.StateType.WORKING);
		}
		e.eventTaskRequest(entry.getId());
		pendingTasks.add(entry);
		//TODO Send only one message?
		try {
			//Notify cluster that the node is going to start the execution of the task
			channel.send(null, new TaskNotification(TaskMessage.MessageType.TASK_STATE,entry));
			//Request the task to the owner
			channel.send(entry.getOwner(), new TaskRequest(TaskMessage.MessageType.TASK_REQUEST, entry.getId()));
		} catch (Exception e1) {
			e.eventError("Request Task Failed. Are you still connected to the cluster?");
			synchronized(entry){
				entry.setState(TaskEntry.StateType.UNDEFINED);
			}
			pendingTasks.remove(entry);
		}

	}
	
	/**
	 * Handles the task and calls {@link #sendResult(Object, TaskEntry)} to return the result via multicast message
	 * @throws Exception 
	 */
	public void handleTask(TaskID id, Task task){
		TaskEntry entry = null;
		for (TaskEntry i : pendingTasks){
			if (i.getId().equals(id)){
				entry = i;
				break;
			}
		}
		if (entry != null){
			pendingTasks.remove(entry);
			e.eventTaskExecution(entry);
			Future<Object> fResult = (Future<Object>) executor.submit(task);
			workingTasks.put(entry, fResult);
			try {
				sendResult(fResult.get(),entry);
			} catch (Exception e1) {
				e.eventError("Problem sending the result");
			}
		}
		else
			e.eventWarning(" Received task " + id.toString() + ", but not in pendingTasks");
	}
	
	/**
	 * Looks for Available tasks. If there is no task for the node, it take a null task or
	 * calls a stealing algorithm to steal from other node.
	 *
	 */
	public synchronized TaskEntry fetchTask(){
		Address localAddress = channel.getAddress();
		for (TaskEntry entry : tasksIndex){
			if (entry.getHandler().equals(localAddress) && entry.getState().equals(TaskEntry.StateType.SUBMITTED))
				return entry;
		}
		TaskEntry result = stlStrat.steal(tasksIndex);
			if (result == null)
				return null;
		synchronized(result){
			result.setHandler(channel.getAddress());
		}
		try {
			channel.send(null, new TaskNotification(TaskMessage.MessageType.TASK_STEAL,result));
		} catch (Exception e1) {
			e.eventError("Steal message failed. Are you still connected to the cluster?");
			return null;
		}
		return result ;
	}
	
	/**
	 * Send a multicast message with the result of a task
	 * @param result: the result of the task
	 * @param entry: the task entry
	 */
	public void sendResult(Object result, TaskEntry entry ){
		synchronized(entry){
			entry.setResult(result);
			entry.setState(TaskEntry.StateType.FINISHED);
		}
		e.eventTaskComplete(entry);
		//Notifies the cluster of the state and the result
		try {
			channel.send(null, new TaskNotification(TaskMessage.MessageType.TASK_RESULT,entry));
		} catch (Exception e1) {
			e.eventError("Send result failed. Are you still connected to the cluster?");
		}
		handleTaskResult(entry);
	}


	public TaskID submit(Task t, long timeout){
		TaskID id = TaskID.newTask(channel.getAddress());
		synchronized(tasksMap){
			tasksMap.put(id, t);
		}
		TaskEntry entry = new TaskEntry(id,null,timeout);
		schStrat.assign(entry, channel.getView());
		//Notify the cluster that a new task exists
		try {
			channel.send(null, new TaskNotification(TaskMessage.MessageType.ADD_TASK,entry));
		} catch (Exception e1) {
			e.eventError("Submit Failed. Are you connected to the cluster?");
			tasksMap.remove(id);
			return null;
		}
		return id;
	}
	
	/**
	 * Checks if the task is completed 
	 */
	public boolean isDone(TaskID id){
		for (TaskEntry entry : tasksIndex)
			if(entry.getId().equals(id))
				if(entry.getState() == TaskEntry.StateType.FINISHED)
					return true;
		return false;
	}
	
	/**
	 * Returns the result of a tasks. MUST be used after {@link #isDone(TaskID)}
	 */
	public Object getResult(TaskID id){
		for (TaskEntry entry : tasksIndex)
			if(entry.getId().equals(id))
				if(entry.getState() == TaskEntry.StateType.FINISHED)
					return entry.getResult();
		return null;
	}
	
	public void viewAccepted(View new_view) {
		if (actualView != null){
			if (new_view.size() < actualView.size()){
				Address missingNode = searchMissingNode(new_view,actualView);
				editTasks(missingNode);
				e.eventNodeCrash(missingNode);
			}
			else{
				Address newNode = searchNewNode(new_view,actualView);
				e.eventNodeAvailable(newNode);
			}
		}
		actualView = new_view;
	}
	
	/**
	 * Searches for the new address and returns it
	 * @param newView
	 * @param oldView
	 * @return the new address
	 */
	private Address searchNewNode(View newView,View oldView){
		List<Address> n = newView.getMembers();
		List<Address> o = oldView.getMembers();
		for (int i=0;i<n.size();i++)
			if (n.get(i).equals(o.get(i)))
				return n.get(i);
		return null;
	}
	
	/**
	 * Searches for the missing address and returns it
	 * @param newView
	 * @param oldView
	 * @return the missing address
	 */
	private Address searchMissingNode(View newView,View oldView){
		List<Address> n = newView.getMembers();
		List<Address> o = oldView.getMembers();
		for (int i=0;i<n.size();i++)
			if (n.get(i).equals(o.get(i)))
				return o.get(i);
		return null;
	}
	/**
	 * Refresh the tasksIndex. If the node that crashed was a owner, the task is removed, if it was the
	 * responsible for a task, it sets the handler to null.
	 * @param address
	 */
	private synchronized void editTasks(Address address){
		for(TaskEntry te : tasksIndex){
			if (te.getOwner().equals(address)){
				tasksIndex.remove(te);
				pendingTasks.remove(te);
				if (workingTasks.containsKey(te)){
					Future ft = workingTasks.get(te);
					ft.cancel(true);
					workingTasks.remove(te);
				}
			}
			if (te.getHandler().equals(address)){
				te.setHandler(null);
				te.setState(TaskEntry.StateType.SUBMITTED);
			}
		}
	}

	public void receive(Message msg) {
		TaskMessage tmsg = (TaskMessage) msg.getObject();
		switch (tmsg.getType()){
		case ADD_TASK:
			handleAddTask(((TaskNotification)tmsg).getEntry());
			break;
		case REMOVE_TASK :
			handleRemoveTask(((TaskNotification)tmsg).getEntry());
			break;
		case GLOBAL_START :
			globalState= WORKING;
			e.eventSystemResume();
			try{
				start();
			}
			catch(Exception e){
				e.printStackTrace();
			}
			break;
		case GLOBAL_PAUSE :
			globalState= PAUSE;
			e.eventSystemPause();
			break;
		case TASK_STEAL :
			if (!msg.getSrc().equals(channel.getAddress()))
				handleSteal(((TaskNotification)tmsg).getEntry());
			break;
		case TASK_STATE :
			if (!msg.getSrc().equals(channel.getAddress()))
				handleTaskUpdate(((TaskNotification)tmsg).getEntry());
			break;
		case TASK_RESULT :
			if (!msg.getSrc().equals(channel.getAddress()))
				handleTaskResult(((TaskNotification)tmsg).getEntry());
			break;
		case TASK_REQUEST :
			TaskID id = ((TaskRequest)tmsg).getId();
			taskResponse(id,msg.getSrc());
			break;
		case TASK_RESPONSE :
			TaskID tId = ((TaskResponse)tmsg).getId();
			Task task = ((TaskResponse)tmsg).getTask() ;
			if (task == null)
				e.eventError("Null task received");
			else
				handleTask(tId, task);
			break;
		default:
			break;
		}
	}
	
	/**
	 * Actions to make when a TASK_STEAL message was received
	 * 	 
	 */
	private void handleSteal(TaskEntry entry){
		TaskEntry oldEntry = null;
		for(TaskEntry e : tasksIndex)
			if (e.equals(entry)){
				oldEntry = e;
				break;
			}
		if (oldEntry == null)
			//The ADD_TASK message hasn't been received yet
			handleAddTask(entry);
		else{
			if(oldEntry.getHandler().equals(channel.getAddress())){
				//MUST check if the task was being handled
				synchronized(pendingTasks){
					pendingTasks.remove(oldEntry);
				}
				synchronized(workingTasks){
					Future<Object> f = workingTasks.get(oldEntry);
						if (f!= null){
						f.cancel(true);
						workingTasks.remove(oldEntry);
						}
				}
			}
			oldEntry.setHandler(entry.getHandler());
			oldEntry.setState(entry.getState());
			e.eventTaskSteal(entry);
			//TODO Improve eventTaskSteal information (old owner)
		}
	}
	
	/**
	 * Actions to make when a TASK_UPDATE message was received
	 * @param entry
	 */
	private void handleTaskUpdate(TaskEntry entry){
		TaskEntry oldEntry = null;
		for(TaskEntry e : tasksIndex)
			if (e.equals(entry)){
				oldEntry = e;
				break;
			}
		if (oldEntry == null)
			//The ADD_TASK message hasn't been received yet
			handleAddTask(entry);
		else {
			if (oldEntry.getState() != TaskEntry.StateType.FINISHED){
				oldEntry.setState(entry.getState());
				e.eventTaskUpdate(entry);
			}
		}
	}
	
	/**
	 * Handle the ADD_TASK message
	 * @param entry
	 */
	private void handleAddTask(TaskEntry entry){
		boolean exists = tasksIndex.contains(entry);
		if (!exists){
			synchronized(tasksIndex){
			tasksIndex.add(entry);
			}
			finished = false;
			e.eventNewTask(entry);
			try {
				start();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}
	
	/**
	 * Handle the REMOVE_TASK message
	 */
	private void handleRemoveTask(TaskEntry entry){
		synchronized(tasksIndex){
			tasksIndex.remove(entry);
		}
		synchronized(pendingTasks){
			pendingTasks.remove(entry);
		}
		synchronized(workingTasks){
			if (workingTasks.containsKey(entry)){
				Future taskF = workingTasks.get(entry);
				taskF.cancel(true);
				workingTasks.remove(entry);
			}
		}
		e.eventRemoveTask(entry.getId());
	}
	
	
	/**
	 * Handle the TASK_RESULT message
	 * @param entry
	 */
	private void handleTaskResult(TaskEntry entry){
		boolean exists = tasksIndex.contains(entry);
		if (exists){
			TaskEntry oldEntry = null;
			for (TaskEntry e : tasksIndex)
				if(e.equals(entry)){
					oldEntry = e;
					break;
				}	
			oldEntry.setResult(entry.getResult());
//			e.eventTaskComplete(oldEntry); 
		}else{
			//The ADD_TASK has not been received yet
			handleAddTask(entry);
		}
		//notify if owner equals local address
		if (entry.getOwner().equals(channel.getAddress()))
			e.eventTaskResult(entry);
	}
	
	/**
	 * Sends the response to the src of the message with the task.
	 * Used when a TaskRequest arrives
	 * @param id
	 * @param src
	 */
	private void taskResponse(TaskID id, Address src){
		if ( id== null)
			e.eventError("Null ID received in a Task Request");
		else{
			try {
				Task t = null;
				synchronized(tasksMap){
					for (TaskID tid : tasksMap.keySet())
						if (tid.equals(id)){
							t = tasksMap.get(tid);
							break;
						}							
//					t = tasksMap.get(id);
				}
				if (t != null){
					channel.send(src, new TaskResponse(t,id));
					e.eventTaskResponse(id);
				}
				else
					e.eventError("Task request with Error, no task assigned to the ID " + id.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void getState(OutputStream output) throws Exception {
		DataOutputStream out = new DataOutputStream(output);
		synchronized(tasksIndex){
			Util.objectToStream(tasksIndex, out);
		}
		Util.objectToStream(globalState, out);
	}
	
	public void setState(InputStream input) throws Exception {
		Vector<TaskEntry> inVector = (Vector<TaskEntry>)Util.objectFromStream(new DataInputStream(input));
		if (inVector != null)
			globalState = (boolean)Util.objectFromStream(new DataInputStream(input));
		synchronized(tasksIndex){
			tasksIndex.addAll(inVector);
		}
	}

	public boolean isGlobalState() {
		return globalState;
	}

	public void setGlobalState(boolean globalState){
		this.globalState = globalState;
		try {
			if (globalState)
				channel.send(null,new TaskMessage(TaskMessage.MessageType.GLOBAL_START));
			else
				channel.send(null,new TaskMessage(TaskMessage.MessageType.GLOBAL_PAUSE));
		} catch (Exception e1) {
			e.eventError("Could not set Global State. Are you still connected to the cluster?");
		}
	}

	public boolean isLocalState() {
		return localState;
	}

	public synchronized void setLocalState(boolean localState) {
		this.localState = localState;
		if (localState == WORKING)
			start();
	}
	
	public Vector<TaskEntry> getTasksIndex(){
		return tasksIndex;
	}
	
	public Map<TaskID,Task> getTasksMap(){
		return tasksMap;
	}

	
	
}

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
	
	
	public Peer(Eventable e,SchedulerStrategy schStrat, StealingStrategy stlStrat, ExecutorService executor){
		this.e = e ;
		this.schStrat = schStrat;
		this.stlStrat = stlStrat;
		this.localState = PAUSE;
		this.finished = false;
		this.executor = executor;
	}

	public void connect(String cluster) throws Exception{
		channel = new JChannel();
		channel.setReceiver(this);
        channel.connect(cluster);
        channel.getState(null, 10000);
	}
	
	public void disconnect(){
		channel.close();
	}
	
	/**
	 * Working loop. It searches for tasks and execute them
	 */
	private synchronized void start() throws Exception{
		while (globalState == WORKING && localState== WORKING && !finished){
			TaskEntry entry = fetchTask();
			if(entry != null)
				requestTask(entry);
			else {
				e.eventLocalPause();
				finished = true;
			}
		}
	}
	
	
	/**
	 * Sends a request to the owner of the task, asking for the task. The answer is handle in {@link #receive(Message)}.
	 * @param entry
	 * @param task
	 */
	public void requestTask(TaskEntry entry) throws Exception{
		synchronized(entry){
			entry.setState(TaskEntry.StateType.WORKING);
		}
		e.eventTaskRequest(entry.getId());
		pendingTasks.add(entry);
		//Notify cluster that the node is going to start the execution of the task
		channel.send(null, new TaskNotification(TaskMessage.MessageType.TASK_STATE,entry));
		//Request the task to the owner
		channel.send(entry.getOwner(), new TaskRequest(TaskMessage.MessageType.TASK_REQUEST, entry.getId()));
	}
	
	/**
	 * Handles the task and calls {@link #sendResult(Object, TaskEntry)} to return the result via multicast message
	 * @throws Exception 
	 */
	public void handle(TaskID id, Task task){
		//TODO Analyze using Executor
		//TODO Check if it is in pendingTasks
		TaskEntry entry = null;
		for (TaskEntry i : pendingTasks){
			if (i.getId().equals(id)){
				entry = i;
				break;
			}
		}
		if (entry != null){
			e.eventTaskExecution(entry);
			Future<Object> fResult = (Future<Object>) executor.submit(task);
			Object result = task.execute();
			try {
				sendResult(result,entry);
			} catch (Exception e) {
				e.printStackTrace();
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
	public synchronized TaskEntry fetchTask() throws Exception{
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
		channel.send(null, new TaskNotification(TaskMessage.MessageType.TASK_STEAL,result));
		return result ;
	}
	
	/**
	 * Send a multicast message with the result of a task
	 * @param result: the result of the task
	 * @param entry: the task entry
	 */
	public void sendResult(Object result, TaskEntry entry ) throws Exception{
		synchronized(entry){
			entry.setResult(result);
			entry.setState(TaskEntry.StateType.FINISHED);
		}
		e.eventTaskComplete(entry);
		//Notifies the cluster of the state and the result
		channel.send(null, new TaskNotification(TaskMessage.MessageType.TASK_RESULT,entry));
	}


	public TaskID submit(Task t, long timeout) throws Exception {
		TaskID id = TaskID.newTask(channel.getAddress());
		synchronized(tasksMap){
			tasksMap.put(id, t);
		}
		TaskEntry entry = new TaskEntry(id,null,timeout);
		schStrat.assign(entry, channel.getView());
		//Notify the cluster that a new task exists
		channel.send(null, new TaskNotification(TaskMessage.MessageType.ADD_TASK,entry));
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
			if (te.getOwner().equals(address))
				tasksIndex.remove(te);
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
			synchronized(tasksIndex){
				tasksIndex.add(((TaskNotification)tmsg).getEntry());
			}
			finished = false;
			e.eventNewTask(((TaskNotification)tmsg).getEntry());
			try {
				start();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			break;
		case REMOVE_TASK :
			synchronized(tasksIndex){
			tasksIndex.remove(((TaskNotification)tmsg).getEntry());
			}
			e.eventRemoveTask(((TaskNotification)tmsg).getEntry().getId());
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
			synchronized(tasksIndex){
				tasksIndex.remove(((TaskNotification)tmsg).getEntry());
				tasksIndex.add(((TaskNotification)tmsg).getEntry());
				if (pendingTasks.contains(((TaskNotification)tmsg).getEntry()))
					pendingTasks.remove(((TaskNotification)tmsg).getEntry());
					
			}
			e.eventTaskSteal(((TaskNotification)tmsg).getEntry());
			break;
		case TASK_STATE :
			synchronized(tasksIndex){
				tasksIndex.remove(((TaskNotification)tmsg).getEntry());
				tasksIndex.add(((TaskNotification)tmsg).getEntry());
			}
			e.eventTaskUpdate(((TaskNotification)tmsg).getEntry());
			break;
		case TASK_RESULT :
			synchronized(tasksIndex){
				tasksIndex.remove(((TaskNotification)tmsg).getEntry());
				tasksIndex.add(((TaskNotification)tmsg).getEntry());
			}
			e.eventTaskComplete(((TaskNotification)tmsg).getEntry());
			break;
		case TASK_REQUEST :
			TaskID id = ((TaskRequest)tmsg).getId();
			taskResponse(id,msg.getSrc());
			break;
		case TASK_RESPONSE :
			TaskID tId = ((TaskResponse)tmsg).getId();
			Task task = ((TaskResponse)tmsg).getTask() ;
			//DEBUG
			if (task == null)
				e.eventError("Null task received");
			else
				handle(tId, task);
			break;
		default:
			break;
		}
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
				e.eventWarning("Checking: " + id.toString());
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

	public void setGlobalState(boolean globalState) throws Exception{
		this.globalState = globalState;
		if (globalState)
			channel.send(null,new TaskMessage(TaskMessage.MessageType.GLOBAL_START));
		else
			channel.send(null,new TaskMessage(TaskMessage.MessageType.GLOBAL_PAUSE));
	}

	public boolean isLocalState() {
		return localState;
	}

	public synchronized void setLocalState(boolean localState) throws Exception{
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

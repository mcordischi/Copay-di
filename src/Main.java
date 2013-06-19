import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.concurrent.Future;

import task.ExceptionTask;
import task.FutureTaskResult;
import task.StringTask;
import node.Master;
import node.MasterNode;
import node.NodeStealSlaveNode;
import node.Slave;
import node.TasksNode;
import algorithm.FirstNodeStealingStrategy;
import algorithm.NodeStealingStrategy;
import algorithm.RandomSchedulerStrategy;
import algorithm.SchedulerStrategy;
import event.DebugInterface;
import event.Eventable;


public class Main {

	public static int masterID = 0;
	public static int slaveID = 0;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
	
		SchedulerStrategy schStrat = new RandomSchedulerStrategy();
		NodeStealingStrategy stlStrat = new FirstNodeStealingStrategy();
		
		BufferedReader in;
		String str; 

		System.out.println("THIS IS A SAMPLE PROGRAM \n"
						+ "Its only purpose is to show the basic functionalities. \n" 
						+ "You can get the source code in http://github.com/tinchofm/Copay-di\n"
						+"You must press enter to display the menu. Press enter to get started..")
						;
		
		
		boolean finish = false;
		Vector< Vector<FutureTaskResult> > futures  = new Vector< Vector<FutureTaskResult> >();
		Vector<Master> masters = new Vector<Master>();
		Vector<Slave> slaves = new Vector<Slave>();
		in=new BufferedReader(new InputStreamReader(System.in));
		while(!finish){
			in.readLine();
			System.out.print("Press next Action :" +
							"\n\tAdd Master(M): There are " + masters.size()  +" masters." +
							"\n\tAdd Slave(S): There are " + slaves.size()  +" slaves." +
							"\n\tRemove Master(RM)." +
							"\n\tRemove Slave(RS)." +
							"\n\tStart system(START)." +
							"\n\tStop system(STOP)."  +
							"\n\tviewTasks(T)" +
							"\n\tviewInfo(I)" +
							"\n\tviewResults(R)" +
							"\n\tsubmit 100 Tasks for each master (U)" +
							"\n\tremove 10 Tasks for each master (D)" +
							"\n\tExit(Q)\n"); System.out.flush();
	        str = in.readLine().toLowerCase();
	        
	        switch(str){
	        case "m": //add master
	        	Eventable diMaster = new DebugInterface("MTR" + masterID);
	        	masterID++;
	        	Master master = new MasterNode(diMaster,schStrat);
	    		master.connect("network");
	    		masters.add(master);
	    		futures.add(new Vector<FutureTaskResult>());
	        	break;
	        case "s": //add slave
	        	Eventable diSlave = new DebugInterface("SLV" + slaveID);
	        	slaveID++;
	        	Slave slave = new NodeStealSlaveNode(diSlave,null,5);
	    		slave.connect("network");
	            slave.setLocalState(true);
	            slaves.add(slave);
	            break;
	        case "rm": //remove master
	        {
	        	Master toRemove = masters.get(0);
	        	masters.remove(0);
	        	futures.remove(0);
	        	toRemove.disconnect();
	        	break;
	        }
        	case "rs": //remove slave
        	{
	        	Slave toRemove = slaves.get(0);
	        	slaves.remove(0);
	        	toRemove.disconnect();
	        	break;
        	}
	        case "start": //start system
	        	if (masters.size() > 0)
	        		masters.get(0).setSystemState(true);
	        	else if (slaves.size() >0)
	        		slaves.get(0).setSystemState(true);
	        	else
	        		System.out.println("There are no Nodes!");
	        	break;
	        case "stop": //stop system
	        	if (masters.size() > 0)
	        		masters.get(0).setSystemState(false);
	        	else if (slaves.size() >0)
	        		slaves.get(0).setSystemState(false);
	        	else
	        		System.out.println("There are no Nodes!");
	        	break;
	        case "t": //viewTasks
	        	if (masters.size() > 0)
	        		((TasksNode)masters.get(0)).notifyTasksIndex();
	        	else if (slaves.size() >0)
	        		((TasksNode)slaves.get(0)).notifyTasksIndex();
	        	else
	        		System.out.println("There are no Nodes!");
	        	break;
	        case "i": //viewInfo
	        	if (masters.size() > 0)
	        		((TasksNode)masters.get(0)).notifyInformation();
	        	else if (slaves.size() >0)
	        		((TasksNode)slaves.get(0)).notifyInformation();
	        	else
	        		System.out.println("There are no Nodes!");
	        	break;
	        case "r": //viewResults
	        	for (int i=0; i< futures.size();i++){
	        		Vector<FutureTaskResult> vector = futures.get(i);
	        		System.out.println("\n\n-----MASTER " + i  + "-----");
		        	for (FutureTaskResult fut : vector){
		        		System.out.print(fut.getTaskID().toString());
		        		if (fut.isDone())
		        			System.out.println(" is done.");
		        		else
		        			System.out.println(" NOT COMPLETED");
		        	}
	        	}
	        	break;
	        case "u": //submit 100 tasks
	        	for (int masterPos = 0 ; masterPos<masters.size();masterPos++)
	        		for (int i= 0 ; i<100;i++){
	        			futures.get(masterPos).add(masters.get(masterPos).submit(new StringTask("T " + i)));
	        		}
	        	break;
	        case "d": // Remove 10 tasks
	        	for (Vector<FutureTaskResult> t: futures)
	        		for(int i=0; i<10;i++)
	        			t.get(i).cancel(true);
	        	break;
	        case "q": // Exit
	        	finish = true;
	        	break;
        	default :
        		System.out.println("Command not valid. Press Enter");
	        }
		}
        
       System.out.println("Goodbye!");
       System.runFinalization();
       
	}

}

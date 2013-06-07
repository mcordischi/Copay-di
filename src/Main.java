import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

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

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Eventable diMaster1 = new DebugInterface("MTR1");
		Eventable diSlave1 = new DebugInterface("SLV1");
		SchedulerStrategy schStrat = new RandomSchedulerStrategy();
		NodeStealingStrategy stlStrat = new FirstNodeStealingStrategy();

		
		Master master1 = new MasterNode(diMaster1,schStrat);
		master1.connect("network");
		
		Slave slave1 = new NodeStealSlaveNode(diSlave1,stlStrat,5);
		slave1.connect("network");
		

        master1.setSystemState(true);
        slave1.setLocalState(true);
		
		BufferedReader in;
		String str; 
//		BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
//		System.out.print("Press enter to start system"); System.out.flush();
//        String str = in.readLine().toLowerCase();
        
		boolean finish = false;
		Vector<FutureTaskResult> set = new Vector<FutureTaskResult>();
		
		while(!finish){
			in=new BufferedReader(new InputStreamReader(System.in));
			System.out.print("Press next Action : \n\tviewTasks(T)\n\tviewInfo(I)\n\tsubmitTasks(S)\n\tremoveTasks(R)\n\tExit(Q)"); System.out.flush();
	        str = in.readLine().toLowerCase();
	        
	        switch(str){
	        case "t":
	        	((TasksNode)slave1).notifyTasksIndex();
	        	break;
	        case "i":
	        	((TasksNode)slave1).notifyInformation();
	        	break;
	        case "s":
	        	for (int i= 0 ; i<10;i++){
	        		set.add(master1.submit(new StringTask("T" + i)));
	        	}
	        	break;
	        case "r":
	        	for (FutureTaskResult t: set)
	        		t.cancel(true);
	        	break;
	        case "q":
	        	finish = true;
	        	break;
	        }
		}
        
       
        
	}

}

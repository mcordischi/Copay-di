import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import algorithm.*;
import event.*;
import task.*;
import node.*;



public class Main {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Eventable diMaster = new DebugInterface("MTR");
		Eventable diSlave = new DebugInterface("SLV");
		SchedulerStrategy schStrat = new RandomSchedulerStrategy();
		TaskStealingStrategy stlStrat = new NullStealingStrategy();

		
		Slave slave = new TaskStealSlaveNode(diSlave,stlStrat,2);
		slave.connect("network");
		
		BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
		
        System.out.print("Press enter connect master"); System.out.flush();
        in.readLine().toLowerCase();
		
		Master master = new MasterNode(diMaster,schStrat);
		master.connect("network");

//        System.out.print("Press enter to Notify information"); System.out.flush();
//        in.readLine().toLowerCase();
//		
//        System.out.println("\n\nMaster: \n");
//        
//		((TasksNode)master).notifyInformation();
//		((TasksNode)master).notifyView();
//		
//		System.out.println("\n\nSLAVE: \n");
//		
//		((TasksNode)slave).notifyInformation();
//		((TasksNode)slave).notifyView();
//		
		
//		System.out.println("Forcing to update ");
//		
//		((TasksNode)slave).forceUpdate();
//		((TasksNode)master).forceUpdate();
//
//		
//        System.out.print("Press enter to Notify information after forced update"); System.out.flush();
//        in.readLine().toLowerCase();
//		
//		((TasksNode)master).notifyInformation();
//		((TasksNode)master).notifyView();
//		
//		System.out.println("\n\nSLAVE: \n");
//		
//		((TasksNode)slave).notifyInformation();
//		((TasksNode)slave).notifyView();
		
		
        System.out.print("Press enter to start loading tasks (Y/n) "); System.out.flush();
        String str = in.readLine().toLowerCase();
        
        Set<FutureTaskResult> set = new HashSet<FutureTaskResult>();
		if (str.startsWith("y")){
			for (int i= 0 ; i<10;i++)
				set.add(master.submit(new StringTask("t" + i)));
			
		}

		
		
        System.out.print("Press enter to start the system"); System.out.flush();
        in.readLine().toLowerCase();
		
		master.setSystemState(Node.WORKING);
		slave.setLocalState(Node.WORKING);
		
		
		for (FutureTaskResult tr : set)
			System.out.println(tr.getTaskID() +  " RESULT " + tr.get().toString());

	}

}

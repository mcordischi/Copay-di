import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import algorithm.*;
import event.*;
import task.*;
import node.*;



public class Main {
	
	static Random random = new Random();
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Eventable diMaster1 = new DebugInterface("MTR1");
		Eventable diMaster2 = new DebugInterface("MTR2");
//		Eventable diSlave2 = new DebugInterface("SLV2");
		SchedulerStrategy schStrat = new RandomSchedulerStrategy();
		//TaskStealingStrategy stlStrat = new NullStealingStrategy();
		NodeStealingStrategy stlStrat = new FirstNodeStealingStrategy();
		
		Vector<Slave> slaves = new Vector<Slave>();
		
		for(int i = 0;i<3;i++){
			Eventable diSlave1 = new DebugInterface("SLV" + i);
			int maxThread= random.nextInt(15) +1 ;
			Slave slave = new NodeStealSlaveNode(diSlave1,stlStrat,maxThread);
			slave.connect("network");
			slaves.add(slave);
		}
		

//		Slave slave2 = new NodeStealSlaveNode(diSlave2,stlStrat,1);
//		slave2.connect("network");
		
		BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
		
        System.out.print("Press enter connect master"); System.out.flush();
        in.readLine().toLowerCase();
		
		Master master1 = new MasterNode(diMaster1,schStrat);
		master1.connect("network");
		
		Master master2 = new MasterNode(diMaster2,schStrat);
		master2.connect("network");
		
        System.out.print("Press enter to Notify information"); System.out.flush();
        in.readLine().toLowerCase();
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
		((TasksNode)slaves.get(0)).notifyInformation();
		((TasksNode)slaves.get(0)).notifyView();
		
		
        System.out.print("Press enter to start loading tasks (Y/n) "); System.out.flush();
        String str = in.readLine().toLowerCase();
        
        Vector<FutureTaskResult> set = new Vector<FutureTaskResult>();
		if (str.startsWith("y")){
			for (int i= 0 ; i<10;i++){
				set.add(master1.submit(new ExceptionTask()));
				set.add(master2.submit(new StringTask("2t" + i)));
			}
		}

		
		
        System.out.print("Press enter to start the system"); System.out.flush();
        in.readLine().toLowerCase();
		
		master1.setSystemState(Node.WORKING);
		
		for (Slave slv: slaves)
			slv.setLocalState(Node.WORKING);
		
		
//		System.out.print("Press enter to start the system"); System.out.flush();
        in.readLine().toLowerCase();
		
        System.out.println("\nTASKS: " + set.size() + "\n\n" );
        
        ((TasksNode)master1).notifyTasksIndex();
        
        System.out.println("\n\n");
        
        in.readLine().toLowerCase();
        
        
		for (FutureTaskResult tr : set)
			if(tr.isDone())
				if (tr.get() != null)
					System.out.println(tr.getTaskID() +  " RESULT " + tr.get().toString());
				else 
					System.out.println(tr.getTaskID() +  " with EXCEPTION " );
			else
				System.out.println("--------> " + tr.getTaskID() + " is not done!!");
        
//		for (FutureTaskResult tr : set)
//			System.out.println(tr.getTaskID() +  " RESULT " + tr.get().toString());

	}

}

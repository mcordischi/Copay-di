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
		Eventable di = new DebugInterface();
		SchedulerStrategy schStrat = new RandomSchedulerStrategy();
		StealingStrategy stlStrat = new NullStealingStrategy();

		
		Slave slave = new TaskStealSlaveNode(di,stlStrat,2);
		slave.connect("network");
		
		BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
		
        System.out.print("Press enter connect master"); System.out.flush();
        in.readLine().toLowerCase();
		
		Master master = new MasterNode(di,schStrat);
		master.connect("network");

        System.out.print("Press enter to Notify information"); System.out.flush();
        in.readLine().toLowerCase();
		
		((TasksNode)master).notifyInformation();
		((TasksNode)master).notifyView();
		
		Vector<Task> tasks = new Vector<Task>();
		
	
        System.out.print("Press enter to start loading tasks (Y/n) "); System.out.flush();
        String str = in.readLine().toLowerCase();
        
        Set<FutureTaskResult> set = new HashSet<FutureTaskResult>();
		if (str.startsWith("y")){
		
			for (int i= 0 ; i<10;i++){
				Task t= new StringTask("t" + i);
				tasks.add(t);
				set.add(master.submit(t));
			}
		}

		
		
        System.out.print("Press enter to start the system"); System.out.flush();
        in.readLine().toLowerCase();
		
		master.setGlobalState(true);
		slave.setLocalState(true);
		
		
		for (FutureTaskResult tr : set)
			System.out.println(tr.getTaskID() +  " RESULT " + tr.get().toString());

	}

}

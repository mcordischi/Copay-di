import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
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
		Peer peer = new Peer(di,schStrat,stlStrat);
		peer.connect("network");
		
		
		Vector<Task> tasks = new Vector<Task>();
		
		BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
        System.out.print("Press enter to start loading tasks (Y/n) "); System.out.flush();
        String str = in.readLine().toLowerCase();
        
        TaskID id = null;
		if (str.startsWith("y")){
		
			for (int i= 0 ; i<1;i++){
				Task t= new StringTask("t" + i);
				tasks.add(t);
				id = peer.submit(t, 0);
			}
		}
		
		for (int i=0; i<1000000; i++)
			;
		
		Map<TaskID,Task> map = peer.getTasksMap();
		
		System.out.println("MAP SIZE :" + map.size());
		
//		for (TaskID i : map.keySet()){
//			System.out.println("TASK: " + ii.toString() + " "+ ((StringTask)map.get(ii)).test());
//		}
		
        System.out.print("Press enter to start the system"); System.out.flush();
        in.readLine().toLowerCase();
		
		peer.setGlobalState(true);
		peer.setLocalState(true);

		while (!peer.isDone(id))
			;
		System.out.println("RESULT: " + peer.getResult(id).toString());
		
	}

}

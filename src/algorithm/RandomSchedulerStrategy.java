package algorithm;

import java.util.Collection;
import java.util.Random;

import node.NodeInformation;

import org.jgroups.View;

import task.TaskEntry;

public class RandomSchedulerStrategy implements SchedulerStrategy {
	
	Random random = new Random();

	@Override
	public void assign(Collection<TaskEntry> tasks,
			Collection<NodeInformation> nodesInfo) {
		for (TaskEntry t: tasks)
			assign(t,nodesInfo);
	}

	@Override
	public void assign(TaskEntry task, Collection<NodeInformation> nodesInfo) {
		Object[] array = nodesInfo.toArray();
		boolean done = false;
		do{
			int position= random.nextInt(array.length) ;
			if (((NodeInformation)array[position]).getNodeType() == NodeInformation.NodeType.SLAVE){
				task.setHandler(((NodeInformation)array[position]).getAddress());
				done = true;
			}
		} while(!done);
	}

}

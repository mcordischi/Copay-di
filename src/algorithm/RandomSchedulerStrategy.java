package algorithm;

import java.util.Collection;
import java.util.Random;

import org.jgroups.View;

import task.TaskEntry;

public class RandomSchedulerStrategy implements SchedulerStrategy {
	
	Random random = new Random();

	@Override
	public void assign(TaskEntry task, View peerList) {
		int position= random.nextInt(peerList.size()) ;
		task.setHandler(peerList.getMembers().get(position));
	}

	@Override
	public void assign(Collection<TaskEntry> tasks, View peerList) {
		for(TaskEntry t : tasks){
			assign(t,peerList);
		}
	}

}

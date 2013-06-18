package task;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Serializable;

import org.jgroups.util.Streamable;
import org.jgroups.util.Util;


/**
 * Just a Task used for testing. It returns a String.
 * @author marto
 *
 */
public class StringTask implements Task,Streamable {

	String result;
	
	public StringTask(String str){
		result = str;
	}
	
	@Override
	public Object call() {
		for (int i=0; i<1000000; i++)
			;
		return result + " FINISHED";
	}
	
	public String test(){
		return result;
	}

	@Override
	public void readFrom(DataInput arg0) throws Exception {
		result = (String) Util.objectFromStream(arg0);
	}

	@Override
	public void writeTo(DataOutput arg0) throws Exception {
		Util.objectToStream(this,arg0);	
		
	}


}

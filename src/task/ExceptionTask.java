package task;


/**
 * Test Class that always throws exception when executed
 * @author marto
 *
 */
public class ExceptionTask implements Task {

	@Override
	public Object call() throws Exception {
		throw new Exception("Just doing my job");
	}

}

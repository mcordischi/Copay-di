package task;

public class ExceptionTask implements Task {

	@Override
	public Object call() throws Exception {
		throw new Exception("Just doing my job");
	}

}

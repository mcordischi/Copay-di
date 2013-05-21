package node;



public interface Slave {
	public abstract void setLocalState(boolean localState);
	public abstract void connect(String Cluster);
}

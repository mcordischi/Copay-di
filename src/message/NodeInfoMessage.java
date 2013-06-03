package message;

import node.NodeInformation;

public class NodeInfoMessage extends TaskMessage {

	NodeInformation info;
	
	public NodeInfoMessage(NodeInformation info){
		super(TaskMessage.MessageType.NODE_INFORMATION);
		this.info = info;
	}
	
	public NodeInformation getInfo(){
		return info;
	}
}

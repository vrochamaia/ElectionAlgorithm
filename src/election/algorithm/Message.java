package election.algorithm;

public class Message {
	

	private int id;
	private String coordinatorIp;
	private MessageType messageType;
	private int messagesQuantity;
	private long electionTime;
	
	public Message(int id,MessageType messageType){
		this.id = id;
		this.messageType = messageType;
		this.messagesQuantity = 0;
		this.electionTime = System.nanoTime();
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public int getMessagesQuantity() {
		return messagesQuantity;
	}

	public void setMessagesQuantity(int messagesQuantity) {
		this.messagesQuantity = messagesQuantity;
	}

	public long getElectionTime() {
		return electionTime;
	}

	public void setElectionTime(long electionTime) {
		this.electionTime = electionTime;
	}

	public String getCoordinatorIp() {
		return coordinatorIp;
	}

	public void setCoordinatorIp(String coordinatorIp) {
		this.coordinatorIp = coordinatorIp;
	}



	public static enum MessageType {
		ELECTION,ELECTED;
	}

}

package hmrc.cds.dms;

public class QueuesDataBean {

	public QueuesDataBean(String queueName, String systemName, String type, String qType, String queueNameToDisplay) {
		super();
		this.queueName = queueName;
		this.systemName = systemName;
		this.type = type;
		this.qType = qType;
		this.queueNameToDisplay = queueNameToDisplay;
	}

	@Override
	public String toString() {
		return "QueuesDataBean [queueName=" + queueName + ", systemName=" + systemName + ", type=" + type + ", qType="
				+ qType + ", queueNameToDisplay=" + queueNameToDisplay + "]";
	}

	String queueName;

	public String getQueueName() {
		return queueName;
	}

	public void setQueueName(String queueName) {
		this.queueName = queueName;
	}

	public String getSystemName() {
		return systemName;
	}

	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}

	public String getqType() {
		return qType;
	}

	public void setqType(String qType) {
		this.qType = qType;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	String systemName;
	String type;
	String qType;
	String queueNameToDisplay;

	public String getQueueNameToDisplay() {
		return queueNameToDisplay;
	}

	public void setQueueNameToDisplay(String queueNameToDisplay) {
		this.queueNameToDisplay = queueNameToDisplay;
	}

}

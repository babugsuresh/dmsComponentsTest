package hmrc.cds.dms;

public class QueuesDataBean {
	
	
	public QueuesDataBean(String queueName, String systemName, String type, String qType) {
		super();
		this.queueName = queueName;
		this.systemName = systemName;
		this.type = type;
		this.qType = qType;
	}
	@Override
	public String toString() {
		return "QueuesDataBean [queueName=" + queueName + ", systemName=" + systemName + ", type=" + type + "]";
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

}

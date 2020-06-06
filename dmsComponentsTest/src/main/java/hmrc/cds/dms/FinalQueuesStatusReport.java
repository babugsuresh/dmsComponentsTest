package hmrc.cds.dms;

import java.util.List;

public class FinalQueuesStatusReport {

	QueuesHeader qheader;
	List<QueuesDepthStatus> qDepthStatus;

	public QueuesHeader getQheader() {
		return qheader;
	}

	public void setQheader(QueuesHeader qheader) {
		this.qheader = qheader;
	}

	public List<QueuesDepthStatus> getqDepthStatus() {
		return qDepthStatus;
	}

	public void setqDepthStatus(List<QueuesDepthStatus> qDepthStatus) {
		this.qDepthStatus = qDepthStatus;
	}

	@Override
	public String toString() {
		return "FinalQueuesStatusReport [qheader=" + qheader + ", qDepthStatus=" + qDepthStatus + "]";
	}

}

class QueuesDepthStatus {

	String envName;
	List<Integer> depths;

	public String getEnvName() {
		return envName;
	}

	public void setEnvName(String envName) {
		this.envName = envName;
	}

	public List<Integer> getDepths() {
		return depths;
	}

	public void setDepths(List<Integer> depths) {
		this.depths = depths;
	}

	@Override
	public String toString() {
		return "QueuesDepthStatus [envName=" + envName + ", depths=" + depths + "]";
	}

}

class QueuesHeader {
	List<String> systemNames;
	List<String> typeOfQueue;
	List<String> type;

	@Override
	public String toString() {
		return "QueuesHeader [systemNames=" + systemNames + ", typeOfQueue=" + typeOfQueue + ", type=" + type + "]";
	}

	public List<String> getSystemNames() {
		return systemNames;
	}

	public void setSystemNames(List<String> systemNames) {
		this.systemNames = systemNames;
	}

	public List<String> getTypeOfQueue() {
		return typeOfQueue;
	}

	public void setTypeOfQueue(List<String> typeOfQueue) {
		this.typeOfQueue = typeOfQueue;
	}

	public List<String> getType() {
		return type;
	}

	public void setType(List<String> type) {
		this.type = type;
	}

}
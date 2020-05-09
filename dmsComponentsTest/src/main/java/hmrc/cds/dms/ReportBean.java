package hmrc.cds.dms;

import java.util.List;

public class ReportBean {
	public String envName;
	public List<ServiceName> serviceNames = null;

	public String getEnvName() {
		return envName;
	}

	public void setEnvName(String envName) {
		this.envName = envName;
	}

	public List<ServiceName> getServiceNames() {
		return serviceNames;
	}

	public void setServiceNames(List<ServiceName> serviceNames) {
		this.serviceNames = serviceNames;
	}

	@Override
	public String toString() {
		return "ReportBean [envName=" + envName + ", serviceNames=" + serviceNames + "]";
	}
}

class ServiceName {

	public String systemName;
	public String serviceName;

	public String getSystemName() {
		return systemName;
	}

	public void setSystemName(String systemName) {
		this.systemName = systemName;
	}

	public List<OperationName> operationNames = null;

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public List<OperationName> getOperationNames() {
		return operationNames;
	}

	public void setOperationNames(List<OperationName> operationNames) {
		this.operationNames = operationNames;
	}

	@Override
	public String toString() {
		return "ServiceName [systemName=" + systemName + ", serviceName=" + serviceName + ", operationNames="
				+ operationNames + "]";
	}

}

class OperationName {

	public String operationName;
	public String status;
	public String response;

	public String getOperationName() {
		return operationName;
	}

	public String getStatus() {
		return status;
	}

	public String getResponse() {
		return response;
	}

	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	@Override
	public String toString() {
		return "OperationName [operationName=" + operationName + ", status=" + status + ", response=" + response + "]";
	}

}

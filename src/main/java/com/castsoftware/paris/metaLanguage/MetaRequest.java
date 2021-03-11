package com.castsoftware.paris.metaLanguage;

public class MetaRequest {

	private String application;
	private String request;
	private String returnValue;
	private MetaRequestType type;

	public MetaRequest(String application, String request, String returnValue, MetaRequestType type) {
		this.application = application;
		this.request = request;
		this.returnValue = returnValue;
		this.type = type;
	}

	public MetaRequest() {
	}

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public String getReturnValue() {
		return returnValue;
	}

	public void setReturnValue(String returnValue) {
		this.returnValue = returnValue;
	}

	public MetaRequestType getType() {
		return type;
	}

	public void setType(MetaRequestType type) {
		this.type = type;
	}

	public String getApplication() {
		return application;
	}

	public void setApplication(String application) {
		this.application = application;
	}
}

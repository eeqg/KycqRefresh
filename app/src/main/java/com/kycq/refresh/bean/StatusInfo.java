package com.kycq.refresh.bean;

public class StatusInfo {
	/** 请求失败 */
	public static final int FAILURE = 1001;
	
	public static final int NETWORK_ERROR = 300;
	
	public int statusCode;
	public String statusContent;
	
	public boolean isSuccessful() {
		return statusCode == 0;
	}
}

package com.sly.demo.webrtc.config;

/**
 * 一对一准备配置
 */
public class OneReadyConfig {
	/** 远端的ID */
	private String remoteId;
	/** 远端是否已经准备好 */
	private boolean status;

	public String getRemoteId() {
		return remoteId;
	}

	public void setRemoteId(String remoteId) {
		this.remoteId = remoteId;
	}

	public boolean isStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	
}

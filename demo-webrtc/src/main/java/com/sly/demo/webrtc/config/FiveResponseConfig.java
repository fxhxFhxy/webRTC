package com.sly.demo.webrtc.config;

/**
 * 五人群组响应配置
 */
public class FiveResponseConfig {
	/** 发送此消息个人编号 */
	private String sendUserId;
	
	private String msg;
	/** 这个房间内其他人的id */
	private String [] userIds;
	/** 房号 */
	private String homeId;
	
	
	public String getHomeId() {
		return homeId;
	}

	public void setHomeId(String homeId) {
		this.homeId = homeId;
	}

	public String[] getUserIds() {
		return userIds;
	}

	public void setUserIds(String[] userIds) {
		this.userIds = userIds;
	}

	public String getSendUserId() {
		return sendUserId;
	}

	public void setSendUserId(String sendUserId) {
		this.sendUserId = sendUserId;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	
}

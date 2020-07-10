package com.demo.webrtc.config;

import java.util.ArrayList;

/**
 * 五人群组配置
 *
 */
public class FiveHomeConfig {
	/** 房号 */
	private String homeId;
	/** 最多允许多少人加入 */
	private final int maxSize = 5;
	/** 已加入的人id */
	private final ArrayList<String> userIds = new ArrayList<String>(maxSize);

	public String getHomeId() {
		return homeId;
	}

	public void setHomeId(String homeId) {
		this.homeId = homeId;
	}

	public ArrayList<String> getUserIds() {
		return userIds;
	}

	public int getMaxSize() {
		return maxSize;
	}
}

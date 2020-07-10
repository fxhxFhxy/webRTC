package com.demo.webrtc.controller;

import com.alibaba.fastjson.JSON;
import com.demo.webrtc.code.MessageTypeCode;
import com.demo.webrtc.config.FiveResponseConfig;
import com.demo.webrtc.config.MessageStructConfig;
import com.demo.webrtc.config.OneReadyResponseConfig;
import com.demo.webrtc.service.SocketService;
import com.demo.webrtc.storage.FiveHomeStorage;
import com.demo.webrtc.storage.MessageSendLockStorage;
import com.demo.webrtc.storage.PersonalSessionStorage;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;

@Component
@ServerEndpoint(value="/websocket")
public class SocketController {
	
	@OnOpen  
    public void open(Session session){  
		String id = session.getId();
		try{
			PersonalSessionStorage.addSessionById(id, session);
			String ret = getRet(MessageTypeCode.PERSONAL_ID, id);
			MessageSendLockStorage.addSendText(id, ret);
		}catch(Throwable e){
			PersonalSessionStorage.delSessionById(id);
			try {
				session.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
    }  
	
    @OnMessage
    public void OnMessage(String message, Session session){
    	try{
	    	MessageStructConfig struct = JSON.parseObject(message,MessageStructConfig.class);
	    	switch (struct.getKey()) {
				//搜索个人
			case QUERY_ID:
				MessageSendLockStorage.addSendText(session.getId(), getRet(MessageTypeCode.QUERY_ID, SocketService.queryId(session.getId(),struct.getValue().toString())));
				break;
				//一对一准备
			case READY_FOR_ONE:
				String remoteId = SocketService.readyForOne(session.getId(), struct.getValue().toString());
				//给对方发送准备请求
				MessageSendLockStorage.addSendText(remoteId,getRet(MessageTypeCode.READY_FOR_ONE,session.getId()));
				break;
				//一对一准备响应
			case READY_FOR_ONE_RESPONSE:
				OneReadyResponseConfig ret = SocketService.readyForOneResponse(session.getId(),Boolean.parseBoolean(struct.getValue().toString()));
				MessageSendLockStorage.addSendText(ret.getOfferId(),getRet(MessageTypeCode.READY_FOR_ONE_RESPONSE, JSON.toJSONString(ret)));
				break;
				//接收发送到服务端的信令
			case SIGNALLING_OFFER:
				OneReadyResponseConfig offerRet = SocketService.signallingOffer(session.getId());
				//转发信令
				if(offerRet.isStatus()){
					MessageSendLockStorage.addSendText(offerRet.getAnswerId(),getRet(MessageTypeCode.SIGNALLING_ONE_ANSWER, JSON.toJSONString(struct.getValue())));
					break;
				}else{
					//一对一找不到对话准备
					//查找是否有指定发送人
					if(struct.getTemp() != null){
						//检查指定发送人是否与当前发送人在同一个房间内
						String responseUserHomeId = FiveHomeStorage.getHomeIdByUserId(struct.getTemp().toString());
						String sendHomeId = FiveHomeStorage.getHomeIdByUserId(session.getId());
						//正常执行
						if(responseUserHomeId != null && sendHomeId != null && responseUserHomeId.equals(sendHomeId)){
							FiveResponseConfig fiveTemp = new FiveResponseConfig();
							fiveTemp.setSendUserId(session.getId());
							fiveTemp.setMsg(struct.getValue().toString());
							MessageSendLockStorage.addSendText(struct.getTemp().toString(),getRet(MessageTypeCode.SIGNALLING_FIVE_ANSWER, JSON.toJSONString(fiveTemp)));
							break;
						}
					}
					MessageSendLockStorage.addSendText(offerRet.getAnswerId(),getRet(MessageTypeCode.READY_FOR_ONE_RESPONSE, JSON.toJSONString(offerRet)));
					break;
				}
				//发送了一对一通道关闭
			case ONE_CHANNEL_CLOSE:
				closeDialogue(session);
				break;
				//创建五人群组房间
			case CREATE_GROUP_FIVE:
				String homeId = SocketService.createHome();
				//自己立马加入到这个房间中
				SocketService.addHome(session.getId(), homeId);
				MessageSendLockStorage.addSendText(session.getId(),getRet(MessageTypeCode.CREATE_GROUP_FIVE,homeId));
				break;
				//退出五人群组房间
			case EXIT_GROUP_FIVE:
				closeDialogue(session);
				break;
				//搜索并加入五人群组房间
			case QUERY_GROUP_FIVE:
				String queryHomeId = struct.getValue().toString();
				boolean addHomeRet = SocketService.addHome(session.getId(), queryHomeId);
				FiveResponseConfig fiveTemp = null;
				//有人进入房间,给房间内其他用户发送消息
				if(addHomeRet){
					ArrayList<String> userIds = FiveHomeStorage.getHomeUsersNotThis(queryHomeId, session.getId());
					//每次的信令,发给所有人一份
					for (String userId : userIds) {
						MessageSendLockStorage.addSendText(userId,getRet(MessageTypeCode.GROUP_FIVE_ADD_USER,session.getId()));
					}
					fiveTemp = new FiveResponseConfig();
					fiveTemp.setSendUserId(session.getId());
					fiveTemp.setMsg(struct.getValue().toString());
					fiveTemp.setHomeId(queryHomeId);
					ArrayList<String> youUserIds = FiveHomeStorage.getHomeUsersNotThis(queryHomeId,session.getId());
					fiveTemp.setUserIds(youUserIds.toArray(new String[youUserIds.size()]));
				}
				//返回房间内其他人的id
				MessageSendLockStorage.addSendText(session.getId(),getRet(MessageTypeCode.QUERY_GROUP_FIVE,fiveTemp != null ? JSON.toJSONString(fiveTemp) : null));
				break;
			default:
				break;
			}
    	}catch(Throwable e){
    		e.printStackTrace();
    	}
    	
    }  

    @OnError
    public void onError(Throwable t) {
    	System.out.println(t.getMessage());
    }

	/**
	 * 当关闭浏览器后,则删除session记录和对话准备
	 * @param session
	 */
	@OnClose
    public void close(Session session){
    	try{
    		closeDialogue(session);
	    	PersonalSessionStorage.delSessionById(session.getId());
    	}catch(Throwable e){
    		e.printStackTrace();
    	}
    }  
    
    /**
     * 删除对话,包括群组 个人
     */
    public void closeDialogue(Session session){
    	//并且给一对一对话 对方 发送关闭对话的通知,如果有在对话
		String closeRemoteId = SocketService.oneChannelClose(session.getId());
		MessageSendLockStorage.addSendText(closeRemoteId,getRet(MessageTypeCode.ONE_CHANNEL_CLOSE,true));
		//先给房内其他人发送退出信息
		String homeId = FiveHomeStorage.getHomeIdByUserId(session.getId());
		if(homeId != null){
			ArrayList<String> userIds = FiveHomeStorage.getHomeUsersNotThis(homeId, session.getId());
			for (String userId : userIds) {
				MessageSendLockStorage.addSendText(userId,getRet(MessageTypeCode.EXIT_GROUP_FIVE,session.getId()));
			}
		}
		SocketService.exitHome(session.getId());
    }
    
    /**
     * 发送消息结构
     * @param key
     * @param msg
     * @return
     */
    private String getRet(MessageTypeCode key,Object msg){
    	MessageStructConfig struct = new MessageStructConfig();
    	struct.setValue(msg);
    	struct.setKey(key);
		return JSON.toJSONString(struct);
    }
    
}

package com.demo.webrtc.storage;

import javax.websocket.Session;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 消息发送锁,其目的是为了防止一个session在上一次的发送还未结束时,又再次执行了发送
 * 引起的一个异常
 *
 */
public class MessageSendLockStorage {
    /**
     * key sessionId  value session上一次发送数据的结果对象
     */
    private static final ConcurrentHashMap<String, Future<Void>> queue = new ConcurrentHashMap<>();

    /**
     * 给一个 一个session 发送消息,
     * 并且检测当前session 上一个消息还未发送完成之前,则堵塞起来
     *
     * @param sendId   要发送给谁
     * @param sendText 发送的内容
     */
    public synchronized static void addSendText(String sendId, String sendText) {
        if (sendId == null) {
            return;
        }
        Future<Void> fu = queue.get(sendId);
        //上一个还没发完,则递归检测
        while (fu != null && !fu.isDone()) {
            try {
                TimeUnit.MICROSECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Session session = PersonalSessionStorage.getSessionById(sendId);
        if (session != null) {
            queue.put(sendId, session.getAsyncRemote().sendText(sendText));
        }
    }
}

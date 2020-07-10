package com.demo.webrtc.storage;

import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 个人session缓存类
 *
 */
public class PersonalSessionStorage {
    private static final ConcurrentHashMap<String, Session> storage = new ConcurrentHashMap<String, Session>();

    /**
     * 通过一个id获得这个session
     *
     * @param id
     * @return
     */
    public static Session getSessionById(String id) {
        return storage.get(id);
    }

    /**
     * 存储一个session 到指定的id
     *
     * @param id
     * @param sio
     */
    public static void addSessionById(String id, Session sio) {
        storage.put(id, sio);
    }

    /**
     * 删除指定的session
     *
     * @param id
     */
    public static void delSessionById(String id) {
        try {
            storage.get(id).close();
            storage.remove(id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

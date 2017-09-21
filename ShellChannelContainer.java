package com.zjlp.face.monitor.entity.dto;

import com.jcraft.jsch.Channel;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author zhangbo 2017年09月13日
 * @reviewer
 */
public class ShellChannelContainer {

    /**
     * 维护所有的管道
     */
    private static ConcurrentMap<String,Channel> channelMap = new ConcurrentHashMap<>();

    /**
     * 维护所有的key
     */
    private static List<String> keys = new CopyOnWriteArrayList<>();

    public static Channel getChannel(String key) {
        return channelMap.get(key);
    }

    public static boolean containsKey(String key){
        return channelMap.containsKey(key);
    }


    /**
     * 如果存在则拿到
     * @param key
     * @param channel
     * @return
     * @aurhor zhangbo 2017年9月13日11:41:34
     */
    public static void putChannel(String key,Channel channel) {
        if(StringUtils.isBlank(key) || channel ==null){
            throw new NullPointerException();
        }
        if(!containsKey(key)){
            channelMap.put(key, channel);
            keys.add(key);
        }
    }

    /**
     * 关闭channel管道,并且移除所有的Channel
     * @aurhor zhangbo 2017年9月13日11:41:34
     */
    public static void closeAndRemoveChannel(String sessionId) {
        for(String key:keys){
            if(key.split("_")[0].startsWith(sessionId)){
                Channel channel = channelMap.get(key);
                if (channel != null){
                    channel.disconnect();
                    channelMap.remove(key);
                }
                keys.remove(key);
            }
        }
    }
}

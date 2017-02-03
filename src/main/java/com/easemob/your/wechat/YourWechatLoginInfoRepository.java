package com.easemob.your.wechat;


import com.fasterxml.jackson.core.JsonProcessingException;

public interface YourWechatLoginInfoRepository {
    YourWechatLoginInfo save(YourWechatLoginInfo info);

    void saveCookie(YourWechatLoginInfo info) throws JsonProcessingException;

    YourWechatLoginInfo find(String id);

    boolean lock(String uin);

    boolean unlock(String uin);

    boolean isOnline(String uin);

    void setOnline(String uin, boolean online);
}

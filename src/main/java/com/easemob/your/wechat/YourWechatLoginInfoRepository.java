package com.easemob.your.wechat;


public interface YourWechatLoginInfoRepository {
    YourWechatLoginInfo save(YourWechatLoginInfo info);
    YourWechatLoginInfo find(String id);
}

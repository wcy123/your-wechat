package com.easemob.your.wechat;


public class YourWechatLoginInfoUtils {
    static String getUin(YourWechatLoginInfo info) {
        return String.valueOf(info.getWebInitResponse().getUser().getUin());
    }
}

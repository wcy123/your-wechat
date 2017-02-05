package com.easemob.your.wechat;


import org.springframework.http.HttpStatus;

public class UinOfflineException extends YourWeChatException {
    public UinOfflineException(String s) {
        super(s);
    }
    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.BAD_GATEWAY;
    }
}

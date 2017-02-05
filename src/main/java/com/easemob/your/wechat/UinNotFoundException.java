package com.easemob.your.wechat;

import org.springframework.http.HttpStatus;

/**
 * Created by wangchunye on 2/5/17.
 */
public class UinNotFoundException extends YourWeChatException {
    public UinNotFoundException(String s) {
        super(s);
    }

    @Override
    public HttpStatus getHttpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}

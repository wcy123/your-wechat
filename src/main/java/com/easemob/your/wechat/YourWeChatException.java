package com.easemob.your.wechat;

import org.springframework.http.HttpStatus;


public abstract class YourWeChatException extends RuntimeException {
    public YourWeChatException(String message) {
        super(message);
    }
    public YourWeChatException(String message, Exception rootCause) {
        super(message, rootCause);
    }

    public abstract HttpStatus getHttpStatus();
}

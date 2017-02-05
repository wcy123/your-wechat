package com.easemob.your.wechat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class YourWechatController {
    @Autowired
    YourWechatLoginService service;

    @RequestMapping("/qrImage.jpg")
    public ResponseEntity<byte[]> getQrImage() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                .header("Cache-Control", "no-cache")
                .body(service.getQrImage());
    }
    @RequestMapping(value = "/{uin}/messages/{toUser}", method = RequestMethod.POST)
    public ResponseEntity<String> sendMsg(
            @PathVariable("uin") String uin,
            @PathVariable("toUser") String toUser,
            @RequestBody String body) {
        service.sendTextMessage(uin, toUser, body);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, String.valueOf(MediaType.TEXT_PLAIN))
                .header("Cache-Control", "no-cache")
                .body("OK");
    }
    @ExceptionHandler({YourWeChatException.class})
    public ResponseEntity handleErrorYourWeChatException(YourWeChatException ex) throws IOException {
        log.error("YourWeChatException", ex);
        return ResponseEntity.status(ex.getHttpStatus())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_PLAIN.toString())
                .body(ex.getMessage());
    }
}

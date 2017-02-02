package com.easemob.your.wechat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
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
}

package com.easemob.your.wechat.impl;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.wcy123.protobuf.your.wechat.WechatProtos;

import com.easemob.your.wechat.HttpCookieJsonDeserializer;
import com.easemob.your.wechat.ProtobufFieldDeserializer;
import com.easemob.your.wechat.ProtobufFieldSerializer;
import com.easemob.your.wechat.YourWechatLoginInfo;
import com.easemob.your.wechat.YourWechatLoginInfoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class YourWechatLoginInfoRepositoryImpl implements YourWechatLoginInfoRepository {
    public static final String PREFIX = "YW:user:";
    private final ObjectMapper mapper;
    @Autowired
    StringRedisTemplate redisTemplate;

    public YourWechatLoginInfoRepositoryImpl() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule()
                .addDeserializer(WechatProtos.WebInitResponse.class,
                        new ProtobufFieldDeserializer(WechatProtos.WebInitResponse.class))
                .addSerializer(WechatProtos.WebInitResponse.class, new ProtobufFieldSerializer<>())
                .addDeserializer(WechatProtos.ContactListResponse.class,
                        new ProtobufFieldDeserializer(WechatProtos.ContactListResponse.class))
                .addSerializer(WechatProtos.ContactListResponse.class,
                        new ProtobufFieldSerializer<>())
                .addDeserializer(HttpCookie.class, new HttpCookieJsonDeserializer())
                .addSerializer(WechatProtos.MemberList.class,
                        new ProtobufFieldSerializer<>())
                .addDeserializer(WechatProtos.MemberList.class, new ProtobufFieldDeserializer(WechatProtos.MemberList.class))

        ;
        mapper.registerModule(module);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    @Override
    public YourWechatLoginInfo save(YourWechatLoginInfo info) {
        try {
            redisTemplate.boundValueOps(
                    getUserKey(info)).set(mapper.writeValueAsString(info));
            for (Map.Entry<String, HttpCookie> cookieEntry : info.getCookies().entrySet()) {
                redisTemplate.boundHashOps(
                        getUserCookieKey(info)).put(cookieEntry.getKey(),
                                mapper.writeValueAsString(cookieEntry.getValue()));
            }
            for (Map.Entry<String, WechatProtos.MemberList> entry : info.getContactList().entrySet()) {
                redisTemplate.boundHashOps(
                        getUserContactKey(info)).put(entry.getValue().getUserName(),
                                mapper.writeValueAsString(entry.getValue()));
            }
        } catch (JsonProcessingException ex) {
            log.error("cannot save {}", ex);
            return info;
        }
        return info;
    }

    private String getUserContactKey(YourWechatLoginInfo info) {
        return getUserKey(info) + ":contact";
    }

    private String getUserCookieKey(YourWechatLoginInfo info) {
        return getUserKey(info) + ":cookie";
    }

    private String getUserKey(YourWechatLoginInfo info) {
        final String s = String.valueOf(info.getWebInitResponse().getUser().getUin());
        return getKey(s);
    }

    private String getKey(String s) {
        return PREFIX + s;
    }

    @Override
    public YourWechatLoginInfo find(String url) {
        throw new UnsupportedOperationException("not supported " + url );
    }
}

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

    public void setRedisTemplate(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    @Override
    public YourWechatLoginInfo save(YourWechatLoginInfo info) {
        try {
            redisTemplate.boundValueOps(
                    getUserKey(getUin(info))).set(mapper.writeValueAsString(info));
            for (Map.Entry<String, HttpCookie> cookieEntry : info.getCookies().entrySet()) {
                redisTemplate.boundHashOps(
                        getUserCookieKey(getUin(info))).put(cookieEntry.getKey(),
                                mapper.writeValueAsString(cookieEntry.getValue()));
            }
            for (Map.Entry<String, WechatProtos.MemberList> entry : info.getContactList().entrySet()) {
                redisTemplate.boundHashOps(
                        getUserContactKey(getUin(info))).put(entry.getValue().getUserName(),
                                mapper.writeValueAsString(entry.getValue()));
            }
            //redisTemplate.boundValueOps(getUserLoginStatus(getUin(info))).set(String.valueOf(info.getLoginned()));
        } catch (JsonProcessingException ex) {
            log.error("cannot save {}", ex);
            return info;
        }
        return info;
    }
    private String getUin(YourWechatLoginInfo info) {
        return String.valueOf(info.getWebInitResponse().getUser().getUin());
    }
    private String getUserContactKey(String uin) {
        return getUserKey(uin) + ":contact";
    }
    private String getUserLoginStatus(String uin) {
        return getUserKey(uin) + ":loginStatus";
    }

    private String getUserCookieKey(String uin) {
        return getUserKey(uin) + ":cookie";
    }

    private String getUserKey(String uin) {
        final String s = String.valueOf(uin);
        return getKey(s);
    }

    private String getKey(String s) {
        return PREFIX + s;
    }

    @Override
    public YourWechatLoginInfo find(String uin) {
        try {
            final YourWechatLoginInfo info = mapper.readValue(redisTemplate.boundValueOps(getUserKey(uin)).get(), YourWechatLoginInfo.class);
            for (Map.Entry<String, String> cookieEntry : redisTemplate.<String, String>boundHashOps(getUserCookieKey(uin)).entries().entrySet()) {
                final HttpCookie httpCookie = mapper.readValue(cookieEntry.getValue(), HttpCookie.class);
                info.getCookies().put(cookieEntry.getKey(), httpCookie);
            }
            for (Map.Entry<String, String> memberListEntry : redisTemplate.<String, String>boundHashOps(getUserContactKey(uin)).entries().entrySet()) {
                final WechatProtos.MemberList memberList = mapper.readValue(memberListEntry.getValue(), WechatProtos.MemberList.class);
                info.getContactList().put(memberListEntry.getKey(), memberList);
            }
            return info;
        } catch (IOException ex) {
            log.error("cannot find {} ",uin, ex);
            return null;
        }
    }
}

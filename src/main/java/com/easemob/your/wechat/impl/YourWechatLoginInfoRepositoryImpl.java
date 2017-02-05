package com.easemob.your.wechat.impl;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.stereotype.Component;
import org.wcy123.protobuf.your.wechat.WechatProtos;

import com.easemob.your.wechat.YourWechatLoginInfo;
import com.easemob.your.wechat.YourWechatLoginInfoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class YourWechatLoginInfoRepositoryImpl implements YourWechatLoginInfoRepository {
    public static final String PREFIX = "YW:user:";
    @Autowired
    ObjectMapper mapper;
    @Autowired
    StringRedisTemplate redisTemplate;

    public YourWechatLoginInfoRepositoryImpl() {

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
            saveCookie(info);
            for (Map.Entry<String, WechatProtos.MemberList> entry : info.getContactList()
                    .entrySet()) {
                redisTemplate.boundHashOps(
                        getUserContactKey(getUin(info))).put(entry.getValue().getUserName(),
                                mapper.writeValueAsString(entry.getValue()));
            }
            // redisTemplate.boundValueOps(getUserLoginStatus(getUin(info))).set(String.valueOf(info.getLoginned()));
        } catch (JsonProcessingException ex) {
            log.error("cannot save {}", ex);
            return info;
        }
        return info;
    }

    @Override
    public void saveCookie(YourWechatLoginInfo info) throws JsonProcessingException {
        for (Map.Entry<String, HttpCookie> cookieEntry : info.getCookies().entrySet()) {
            redisTemplate.boundHashOps(
                    getUserCookieKey(getUin(info))).put(cookieEntry.getKey(),
                            mapper.writeValueAsString(cookieEntry.getValue()));
        }
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

    private String getUserLockKey(String uin) {
        return getUserKey(uin) + ":lock";
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
            final String content = redisTemplate.boundValueOps(getUserKey(uin)).get();
            if (content == null) {
                return null;
            }
            final YourWechatLoginInfo info = mapper.readValue(content, YourWechatLoginInfo.class);
            for (Map.Entry<String, String> cookieEntry : redisTemplate
                    .<String, String>boundHashOps(getUserCookieKey(uin)).entries().entrySet()) {
                final HttpCookie httpCookie =
                        mapper.readValue(cookieEntry.getValue(), HttpCookie.class);
                info.getCookies().put(cookieEntry.getKey(), httpCookie);
            }
            for (Map.Entry<String, String> memberListEntry : redisTemplate
                    .<String, String>boundHashOps(getUserContactKey(uin)).entries().entrySet()) {
                final WechatProtos.MemberList memberList =
                        mapper.readValue(memberListEntry.getValue(), WechatProtos.MemberList.class);
                info.getContactList().put(memberListEntry.getKey(), memberList);
            }
            return info;
        } catch (IOException ex) {
            log.error("cannot find {} ", uin, ex);
            return null;
        }
    }

    @Override
    public boolean lock(String uin) {
        RedisAtomicLong redisAtomicLong =
                new RedisAtomicLong(getUserLockKey(uin), redisTemplate.getConnectionFactory());
        final boolean b = redisAtomicLong.compareAndSet(0, 1);
        redisAtomicLong.expire(1, TimeUnit.SECONDS);
        return b;
    }

    @Override
    public boolean unlock(String uin) {
        RedisAtomicLong redisAtomicLong =
                new RedisAtomicLong(getUserLockKey(uin), redisTemplate.getConnectionFactory());
        redisAtomicLong.set(0);
        return true;
    }

    @Override
    public boolean isOnline(String uin) {
        RedisAtomicLong redisAtomicLong =
                new RedisAtomicLong(getUserLoginStatus(uin), redisTemplate.getConnectionFactory());
        return redisAtomicLong.get() == 1;
    }

    @Override
    public void setOnline(String uin, boolean online) {
        RedisAtomicLong redisAtomicLong =
                new RedisAtomicLong(getUserLoginStatus(uin), redisTemplate.getConnectionFactory());
        redisAtomicLong.set(online ? 1 : 0);
    }

    @Override
    public void delete(YourWechatLoginInfo loginInfo) {
        String uin = getUin(loginInfo);
        redisTemplate.delete(getUserContactKey(uin));
        redisTemplate.delete(getUserLoginStatus(uin));
        redisTemplate.delete(getUserCookieKey(uin));
        redisTemplate.delete(getUserLockKey(uin));
        redisTemplate.delete(getUserKey(uin));
    }
}

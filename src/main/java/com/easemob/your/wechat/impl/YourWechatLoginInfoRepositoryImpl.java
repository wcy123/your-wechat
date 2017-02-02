package com.easemob.your.wechat.impl;

import com.easemob.your.wechat.ProtobufFieldDeserializer;
import com.easemob.your.wechat.ProtobufFieldSerializer;
import com.easemob.your.wechat.YourWechatLoginInfo;
import com.easemob.your.wechat.YourWechatLoginInfoRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.wcy123.protobuf.your.wechat.WechatProtos;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class YourWechatLoginInfoRepositoryImpl implements YourWechatLoginInfoRepository {
    public static final String PREFIX = "YW:user:";
    @Autowired
    StringRedisTemplate redisTemplate;
    private final ObjectMapper mapper;
    public YourWechatLoginInfoRepositoryImpl() {
        mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule()
                .addDeserializer(WechatProtos.Root.class, new ProtobufFieldDeserializer(WechatProtos.Root.class));
        module.addSerializer(WechatProtos.Root.class, new ProtobufFieldSerializer<>());
        mapper.registerModule(module);
    }

    @Override
    public YourWechatLoginInfo save(YourWechatLoginInfo info) {
        try {
            redisTemplate.boundValueOps(getKey(String.valueOf(info.getWebInitResponse().getUser().getUin())))
                    .set(mapper.writeValueAsString(info));
        } catch (JsonProcessingException ex) {
            log.error("cannot save {}", ex);
            return info;
        }
        return info;
    }

    private String getKey(String s) {
        return PREFIX + s;
    }

    @Override
    public YourWechatLoginInfo find(String url) {
        try {
            return mapper.readValue(redisTemplate.boundValueOps(getKey(url)).get(), YourWechatLoginInfo.class);
        } catch (IOException e) {
            log.error("cannot save {}", e);
            return null;
        }
    }
}

package com.easemob.your.wechat;

import com.google.common.collect.ImmutableMap;

import com.easemob.your.wechat.impl.YourWechatLoginInfoRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.wcy123.protobuf.your.wechat.WechatProtos;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import redis.clients.jedis.JedisPoolConfig;

import static org.junit.Assert.*;


@Ignore
public class YourWechatLoginInfoTest {
    final YourWechatLoginInfoRepositoryImpl yourWechatLoginInfoRepository = new YourWechatLoginInfoRepositoryImpl();

    @Test
    public void main1() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = "sample.json";
        File file = new File(classLoader.getResource(fileName).getFile());
        final FileInputStream stream = new FileInputStream(file);
        final ObjectMapper mapper = yourWechatLoginInfoRepository.getMapper();
        YourWechatLoginInfo loginInfo = mapper.readValue(stream, YourWechatLoginInfo.class);
        stream.close();

        final String json = mapper.writeValueAsString(loginInfo);
        System.out.println(json);
        YourWechatLoginInfo loginInfo2 =  mapper.readValue(json, YourWechatLoginInfo.class);
        Assert.assertEquals(loginInfo, loginInfo2);
    }

    @Test
    public void main2() throws Exception {
        final YourWechatLoginInfo loginInfo = loadInfo();

        System.out.println(loginInfo);;
    }

    private YourWechatLoginInfo loadInfo() {
        final String uin = "1553301943";
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        final JedisConnectionFactory connectionFactory = new JedisConnectionFactory(poolConfig);
        connectionFactory.afterPropertiesSet();
        final StringRedisTemplate redisTemplate =  new StringRedisTemplate(connectionFactory);
        yourWechatLoginInfoRepository.setRedisTemplate(redisTemplate);
        return yourWechatLoginInfoRepository.find(uin);
    }


}

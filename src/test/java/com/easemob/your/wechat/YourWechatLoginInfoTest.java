package com.easemob.your.wechat;

import com.easemob.your.wechat.impl.YourWechatLoginInfoRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.junit.Assert;
import org.junit.Test;
import org.wcy123.protobuf.your.wechat.WechatProtos;

import java.io.File;
import java.io.FileInputStream;

import static org.junit.Assert.*;


public class YourWechatLoginInfoTest {
    @Test
    public void main1() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        String fileName = "sample.json";
        File file = new File(classLoader.getResource(fileName).getFile());
        final FileInputStream stream = new FileInputStream(file);
        final YourWechatLoginInfoRepositoryImpl yourWechatLoginInfoRepository = new YourWechatLoginInfoRepositoryImpl();
        final ObjectMapper mapper = yourWechatLoginInfoRepository.getMapper();
        YourWechatLoginInfo loginInfo = mapper.readValue(stream, YourWechatLoginInfo.class);
        stream.close();

        final String json = mapper.writeValueAsString(loginInfo);
        System.out.println(json);
        YourWechatLoginInfo loginInfo2 =  mapper.readValue(json, YourWechatLoginInfo.class);
        Assert.assertEquals(loginInfo, loginInfo2);
    }


}

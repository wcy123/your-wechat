package com.easemob.your.wechat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

/**
 * Created by wangchunye on 1/31/17.
 */
public class DemoJAX {
    @Test
    public void main1() throws Exception {
        final String s = "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxnewloginpage?ticket=AXXx45zfIH8qCds4vA4veCW1@qrticket_0&uuid=YYdAdwzwmA%3D%3D&lang=zh_CN&scan=1485863958";
        final RestTemplate restTemplate = new RestTemplate();
        final Jaxb2RootElementHttpMessageConverter converter = new Jaxb2RootElementHttpMessageConverter();
        converter.setSupportedMediaTypes(ImmutableList.of(MediaType.ALL));
        restTemplate.setMessageConverters(Collections.singletonList(converter));
        final ResponseEntity<ApiBaseResponse> exchange = restTemplate.exchange(s, HttpMethod.GET, HttpEntity.EMPTY, ApiBaseResponse.class);
        System.out.println(exchange);
    }

    @Test
    public void main2() throws Exception {
        final String s = "<error><ret>0</ret><message></message><skey>@crypt_10a3f884_d1fb0754f115d8abcc9d4fca6af70b13</skey><wxsid>mMv4jpvkSCA5XbnS</wxsid><wxuin>1553301943</wxuin><pass_ticket>QEEN1KMh%2FnR60l0PkAeMAqHOg0BpZjr1AHdsgkpBC%2FOwux9PsJEBkwdsRNG0%2Bt6W</pass_ticket><isgrayscale>1</isgrayscale></error>";
        JAXBContext jaxbContext = JAXBContext.newInstance(ApiBaseResponse.class);

        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        ApiBaseResponse unmarshal = (ApiBaseResponse) jaxbUnmarshaller.unmarshal(new ByteArrayInputStream(s.getBytes()));
        System.out.println(unmarshal);

    }
    /*
     // the following code raises an exception e.g.
            //    [org.xml.sax.SAXParseException; lineNumber: 1; columnNumber: 10; DOCTYPE is disallowed when the feature "http://apache.org/xml/features/disallow-doctype-decl" set to true.]
            final ResponseEntity<byte[]> exchange = restTemplate.exchange(loginInfo.getRawUrl(), HttpMethod.GET, HttpEntity.EMPTY, byte[].class);
            final byte[] body = exchange.getBody();
            log.info("getting baseresponse in string: {}", new String(body, Charset.forName("UTF-8")));
            //final String s = "<error><ret>0</ret><message></message><skey>@crypt_10a3f884_d1fb0754f115d8abcc9d4fca6af70b13</skey><wxsid>mMv4jpvkSCA5XbnS</wxsid><wxuin>1553301943</wxuin><pass_ticket>QEEN1KMh%2FnR60l0PkAeMAqHOg0BpZjr1AHdsgkpBC%2FOwux9PsJEBkwdsRNG0%2Bt6W</pass_ticket><isgrayscale>1</isgrayscale></error>";
            JAXBContext jaxbContext = JAXBContext.newInstance(ApiBaseResponse.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            ApiBaseResponse unmarshal = (ApiBaseResponse) jaxbUnmarshaller.unmarshal(new ByteArrayInputStream(body));
            loginInfo.setBaseResponse(unmarshal);
     */
}

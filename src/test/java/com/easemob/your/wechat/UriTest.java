package com.easemob.your.wechat;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpHeaders;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

/**
 * Created by wangchunye on 1/25/17.
 */
@Ignore

public class UriTest {
    @Test
    public void main1() throws Exception {
        String rawUrl =
                "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxnewloginpage?ticket=ATIszvj5TJdLncw3uNiWChl-@qrticket_0&uuid=Ydx9hdFIvA==&lang=zh_CN&scan=1485334116";
        URI uri = new URI(rawUrl);
        System.out.println(uri.getPath());
        System.out.println(uri.getQuery());
        String query = uri.getQuery();

        final Map<String, String> map = StreamSupport
                .stream(Splitter.on('&').trimResults().split(query).spliterator(), false)
                .map(q -> {
                    int idx = q.indexOf('=');
                    return ImmutableList.of(q.substring(0, idx), q.substring(idx + 1));
                })
                .collect(Collectors.toMap(l -> l.get(0), l -> l.get(1)));
        System.out.println(map);
        System.out.println(uri.getPath());
        System.out.println(uri.resolve("."));
        System.out.println(uri.resolve(".").resolve("webwxinit/abc"));
        System.out.println("e" + String.valueOf(Math.random()).substring(2));
    }

    @Test
    public void main2() throws Exception {
        final YourWechatLoginInfo info = new YourWechatLoginInfo();
        info.updateCookies(Stream.of(
                "Set-Cookie: wxuin=1553301943; Domain=wx.qq.com; Path=/; Expires=Wed, 01-Feb-2017 02:57:24 GMT",
                "Set-Cookie: wxsid=xyJVNKN+oSYaszPq; Domain=wx.qq.com; Path=/; Expires=Wed, 01-Feb-2017 02:57:24 GMT",
                "Set-Cookie: wxloadtime=1485874644; Domain=wx.qq.com; Path=/; Expires=Wed, 01-Feb-2017 02:57:24 GMT"));
        final ObjectMapper mapper = new ObjectMapper();
        final String s = mapper.writeValueAsString(info);
        System.out.println(s);
        final YourWechatLoginInfo info2 = mapper.readValue(s, YourWechatLoginInfo.class);
        System.out.println(info2);

        final HttpHeaders httpHeaders = info2.generateCookieHeader();
        System.out.println(httpHeaders);
    }


}

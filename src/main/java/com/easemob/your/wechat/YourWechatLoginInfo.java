package com.easemob.your.wechat;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.http.HttpHeaders;
import org.wcy123.protobuf.your.wechat.WechatProtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.node.ArrayNode;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class YourWechatLoginInfo {
    private String rawUrl;
    private URI baseUrl;
    private Map<String, String> uriQueryMap;
    private String fileUrl = null;
    private String syncUrl = null;
    private Boolean loginned = false;
    private String deviceId = null;
    private ApiBaseResponse baseResponse;
    @JsonIgnore
    private WechatProtos.WebInitResponse webInitResponse;
    @JsonIgnore
    private Map<String, WechatProtos.MemberList> contactList = new HashMap<>();
    @JsonIgnore
    private Map<String, HttpCookie> cookies = new HashMap<>();

    public void setUrl(String rawUrl) {
        initRawAndBaseUrl(rawUrl);
        initUriQueryMap();
        initDeviceId();
        initFileAndSyncUrl();
    }

    public void initCookie(Stream<String> headers) {
        final Function<String, List<HttpCookie>> parser = HttpCookie::parse;
        final Predicate<HttpCookie> hasExpired = HttpCookie::hasExpired;
        cookies.putAll(headers.flatMap(parser.andThen(Collection::stream))
                .filter(hasExpired.negate())
                .collect(Collectors.toMap(HttpCookie::getName, e-> e)));
    }

    private void initFileAndSyncUrl() {
        for (Map.Entry<String, ImmutableList<String>> e : ImmutableMap
                .<String, ImmutableList<String>>of(
                        "wx2.qq.com", ImmutableList.of("file.wx2.qq.com", "webpush.wx2.qq.com"),
                        "wx8.qq.com", ImmutableList.of("file.wx8.qq.com", "webpush.wx8.qq.com"),
                        "qq.com", ImmutableList.of("file.wx.qq.com", "webpush.wx.qq.com"),
                        "web2.wechat.com",
                        ImmutableList.of("file.web2.wechat.com", "webpush.web2.wechat.com"),
                        "wechat.com",
                        ImmutableList.of("file.web.wechat.com", "webpush.web.wechat.com"))
                .entrySet()) {
            if (baseUrl.getPath().contains(e.getKey())) {
                fileUrl = mmwebwxBin(e.getValue().get(0));
                syncUrl = mmwebwxBin(e.getValue().get(1));
                break;
            }
        }
    }

    private void initRawAndBaseUrl(String rawUrl) {
        this.rawUrl = rawUrl;
        try {
            this.baseUrl = new URI(rawUrl).resolve(".");
        } catch (URISyntaxException e) {
            log.error("cannot parse url {}", rawUrl, e);
        }
    }

    private void initDeviceId() {
        deviceId = "e" + String.valueOf(Math.random()).substring(2);
    }

    private void initUriQueryMap() {
        final int i = rawUrl.indexOf('?');
        if (i > 0) {
            final String queryString = rawUrl.substring(i + 1);
            this.uriQueryMap = StreamSupport
                    .stream(Splitter.on('&').trimResults().split(queryString).spliterator(), false)
                    .map(q -> {
                        int idx = q.indexOf('=');
                        return ImmutableList.of(q.substring(0, idx), q.substring(idx + 1));
                    })
                    .collect(Collectors.toMap(l -> l.get(0), l -> l.get(1)));
        } else {
            this.uriQueryMap = ImmutableMap.of();
        }

    }

    public String getRelativeUrl(String x) {
        return baseUrl.resolve(x).toString();
    }

    private String mmwebwxBin(String s) {
        return "https://" + s + "/cgi-bin/mmwebwx-bin";
    }

    public HttpHeaders generateCookieHeader() {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.COOKIE,
                cookies.entrySet().stream().map(c -> c.getValue().getName() + "=" + c.getValue().getValue())
                        .collect(Collectors.joining(";")));
        return httpHeaders;
    }

    public void mergeContactList(WechatProtos.ContactListResponse contactListResponse) {
        for (WechatProtos.MemberList memberList : contactListResponse.getMemberListList()) {
            contactList.put(memberList.getUserName(), memberList);
        }
    }
}

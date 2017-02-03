package com.easemob.your.wechat;

import static com.easemob.your.wechat.YourWechatLoginInfoUtils.getUin;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.wcy123.protobuf.your.wechat.WechatProtos;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.google.common.collect.ImmutableMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WechatLoginApi {
    private final static Pattern pattern =
            Pattern.compile("window.QRLogin.code = 200; window.QRLogin.uuid = \"(\\S+?)\"");
    private final static Pattern patternSync =
            Pattern.compile("window.synccheck=\\{retcode:\"(\\d+)\",selector:\"(\\d+)\"\\}");
    @Value("${weichat.appid}")
    private String appid;
    @Value("${weixin.login.base.url}")
    private String weixinBaseUrl;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private YourWechatLoginInfoRepository wechatLoginInfoRepository;

    public WechatLoginApi() {}

    public Optional<String> getQrUuid() {
        String s = getQrUuidInternal(appid, "new");
        final Matcher matcher = pattern.matcher(s);
        String result = null;
        if (matcher.find()) {
            final String code = matcher.group(1);
            log.info("create a new qrcode {}", code);
            return Optional.of(code);
        } else {
            log.warn("cannot get QR uuid: result = {}", s);
        }
        return Optional.empty();
    }

    public Optional<byte[]> getQrImage(String qrcode) {
        log.info("obtaing qr image for code({})", qrcode);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.IMAGE_JPEG));

            HttpEntity<String> entity = new HttpEntity<String>(null, headers);

            final ResponseEntity<byte[]> bytesResponse = restTemplate.exchange(
                    getQrUrl(qrcode), HttpMethod.GET,
                    entity,
                    byte[].class);
            final byte[] bytes = bytesResponse.getBody();
            log.info("obtaing qr image for code({}) ok, {} bytes received", qrcode, bytes.length);
            return Optional.of(bytes);
        } catch (RestClientException ex) {
            log.info("obtaing qr image for code({}) failed", qrcode, ex);
            return Optional.empty();
        }
    }

    public String getQrUrl(String uuid) {
        return weixinBaseUrl + "/qrcode/" + uuid;
    }

    private String getRootUrl(String relativeUrl) {
        return weixinBaseUrl + relativeUrl;
    }

    public Optional<String> checkQrCode(String code) {
        try {
            String loginStatus = checkLogin(code);
            log.info("status changed. code=\"{}\" status=\"{}\"", code, loginStatus);
            return Optional.of(loginStatus);
        } catch (RestClientException ex) {
            log.warn("no status code. code=\"{}\" status=\"{}\"", code, ex);
            return Optional.empty();
        }
    }

    public Optional<YourWechatLoginInfo> webInitStep1(YourWechatLoginInfo loginInfo) {
        try {
            log.info("getting baseresponse: {}", loginInfo);
            final ResponseEntity<ApiBaseResponse> exchange = restTemplate.exchange(
                    loginInfo.getRawUrl(), HttpMethod.GET, HttpEntity.EMPTY, ApiBaseResponse.class);
            loginInfo.setBaseResponse(exchange.getBody());
            final List<String> cookieStrings = exchange.getHeaders().get(HttpHeaders.SET_COOKIE);
            loginInfo.updateCookies(cookieStrings.stream());
            log.info("getting baseresponse is done: {}", loginInfo);
            return Optional.of(loginInfo);
        } catch (RestClientException ex) {
            log.warn("cannot getting baseresponse: {}", loginInfo);
            return Optional.empty();
        }

    }

    public Optional<YourWechatLoginInfo> webInitStep2(YourWechatLoginInfo loginInfo) {
        try {
            log.info("webinit step2: {}", loginInfo);
            final long localTime = System.currentTimeMillis();
            WechatProtos.WebInitResponse body = request(loginInfo, HttpMethod.POST,
                    loginInfo.getRelativeUrl("webwxinit"), ImmutableMap.of("_", localTime),
                    new HttpHeaders(),
                    ApiBaseRequest.builder()
                            .baseRequest(loginInfo.buildBaseRequest())
                            .build(),
                    WechatProtos.WebInitResponse.class);
            loginInfo.setWebInitResponse(body);
            loginInfo.setSyncKey(body.getSyncKey());
            log.info("webinit step2: : {}", getUin(loginInfo));
            return Optional.of(loginInfo);
        } catch (RestClientException ex) {
            log.warn("cannot webinit step2: {}", loginInfo, ex);
            return Optional.empty();
        }
    }

    public Optional<YourWechatLoginInfo> showMobileLogin(YourWechatLoginInfo loginInfo) {
        try {
            log.info("showMobileLogin: {}", getUin(loginInfo));
            final HttpHeaders headers = new HttpHeaders();
            final long localTime = System.currentTimeMillis();
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);

            String body = request(loginInfo,
                    HttpMethod.POST, loginInfo.getRelativeUrl("webwxstatusnotify"),
                    ImmutableMap.of("lang", "zh_CN",
                            "pass_ticket", loginInfo.getBaseResponse().getPass_ticket()),
                    headers,
                    ImmutableMap.of(
                            "BaseRequest", loginInfo.buildBaseRequest(),
                            "Code", 3,
                            "FromUserName", loginInfo.getWebInitResponse().getUser().getUserName(),
                            "ToUserName", loginInfo.getWebInitResponse().getUser().getUserName(),
                            "ClientMsgId", localTime),
                    String.class);
            log.info("showMobileLogin: {} {}", getUin(loginInfo), body);
            return Optional.of(loginInfo);
        } catch (RestClientException ex) {
            log.warn("showMobileLogin error {}", loginInfo, ex);
            return Optional.empty();
        }
    }

    public Optional<YourWechatLoginInfo> retrieveContactList(YourWechatLoginInfo loginInfo) {
        try {
            log.info("start retrieveContactList: {}", getUin(loginInfo));
            final long localTime = System.currentTimeMillis();
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            WechatProtos.ContactListResponse body =
                    request(loginInfo,
                            HttpMethod.GET,
                            loginInfo.getRelativeUrl("webwxgetcontact"),
                            ImmutableMap.of(
                                    "r", localTime,
                                    "seq", 0,
                                    "skey", loginInfo.getBaseResponse().getSkey()),
                            headers,
                            ImmutableMap.of(
                                    "BaseRequest", loginInfo.buildBaseRequest(),
                                    "Code", 3,
                                    "FromUserName",
                                    loginInfo.getWebInitResponse().getUser().getUserName(),
                                    "ToUserName",
                                    loginInfo.getWebInitResponse().getUser().getUserName(),
                                    "ClientMsgId", localTime),
                            WechatProtos.ContactListResponse.class);
            log.info("end retrieveContactList:  {}", getUin(loginInfo));
            loginInfo.mergeContactList(body);
            return Optional.of(loginInfo);
        } catch (RestClientException ex) {
            log.warn("showMobileLogin error {}", loginInfo, ex);
            return Optional.empty();
        }
    }

    // internal apis
    private String getQrUuidInternal(String appid, String fun) {
        final URI uri = UriComponentsBuilder.fromHttpUrl(getRootUrl("/jslogin"))
                .queryParam("appid", appid)
                .queryParam("fun", "new")
                .build()
                .toUri();
        return restTemplate.getForObject(uri, String.class);
    }

    private String checkLogin(String code) {
        log.info("checking login for {}", code);
        final String url = getRootUrl("/cgi-bin/mmwebwx-bin/login");
        final long localTime = System.currentTimeMillis();
        URI uri = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("loginicon", "true")
                .queryParam("uuid", code)
                .queryParam("tip", "0")
                .queryParam("r", localTime / 1579) // WTF
                .queryParam("_", localTime)
                .build()
                .toUri();
        // ex1: "window.code=408;"
        // ex2: "url":
        // "https://wx.qq.com/cgi-bin/mmwebwx-bin/webwxnewloginpage?ticket=ATIszvj5TJdLncw3uNiWChl-@qrticket_0&uuid=Ydx9hdFIvA==&lang=zh_CN&scan=1485334116",
        return restTemplate.getForObject(uri, String.class);
    }

    private String webInitInternal(YourWechatLoginInfo loginInfo) {
        log.info("web init for {}", loginInfo);
        final URI uri = UriComponentsBuilder.fromHttpUrl(loginInfo.getRelativeUrl("webwxinit"))
                .queryParam("r", System.currentTimeMillis() / 1000)
                .build()
                .toUri();
        // return restTemplate.exchange();
        return null;
    }

    public Optional<String> checkSync(YourWechatLoginInfo loginInfo) {
        try {
            log.info("checkSync: {}", getUin(loginInfo));
            final HttpHeaders headers = loginInfo.generateCookieHeader();
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            final long localTime = System.currentTimeMillis();
            String body =
                    request(loginInfo,
                            HttpMethod.GET,
                            loginInfo.getRelativeSyncUrl("synccheck"),
                            ImmutableMap.of(
                                    "sid", loginInfo.getBaseResponse().getWxsid(),
                                    "uin", loginInfo.getBaseResponse().getWxuin(),
                                    "deviceid", loginInfo.getDeviceId(),
                                    "synckey", loginInfo.syncKeyAsString()),
                            headers,
                            null,
                            String.class);
            log.info("checkSync:  {} -> {}", getUin(loginInfo), body);
            final Matcher matcher = patternSync.matcher(body);
            if (matcher.find()) {
                if ("0".equals(matcher.group(1))) {
                    return Optional.of(matcher.group(2));
                }
            }
        } catch (RestClientException ex) {
            log.warn("showMobileLogin error {}", loginInfo, ex);
        }
        return Optional.empty();
    }

    public Optional<YourWechatLoginInfo> syncMsg(YourWechatLoginInfo loginInfo) {
        try {
            log.info("syncMsg: {}", getUin(loginInfo));
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            final long localTime = System.currentTimeMillis() / 1000;
            final WechatProtos.SyncMessageResponse body =
                    request(loginInfo,
                            HttpMethod.POST,
                            loginInfo.getRelativeUrl("webwxsync"),
                            ImmutableMap.of(
                                    "sid", loginInfo.getBaseResponse().getWxsid(),
                                    "skey", loginInfo.getBaseResponse().getSkey(),
                                    "pass_ticket", loginInfo.getBaseResponse().getPass_ticket()),
                            headers,
                            ImmutableMap.of(
                                    "BaseRequest", loginInfo.buildBaseRequest(),
                                    "SyncKey", loginInfo.getSyncKey(),
                                    "rr", ~localTime),
                            WechatProtos.SyncMessageResponse.class);
            loginInfo.setSyncKey(body.getSyncCheckKey());
            log.info("syncMsg:  {} -> {}", getUin(loginInfo), loginInfo.syncKeyAsString());
            return Optional.of(loginInfo);
        } catch (Exception ex) {
            log.warn("showMobileLogin error {}", loginInfo, ex);
        }
        return Optional.empty();
    }

    public <T> T request(YourWechatLoginInfo loginInfo,
            HttpMethod method,
            String url,
            Map<String, Object> queryParam,
            HttpHeaders headers,
            Object body,
            Class<T> clazz) {
        final String urlWithQuery =
                queryParam.isEmpty() ? url : url + "?" + queryParam.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&"));
        try {
            log.info("http request {} {}", method, urlWithQuery);
            log.debug("http request {}", body);
            final HttpHeaders headersWithCookies = loginInfo.generateCookieHeader();
            headersWithCookies.putAll(headers);
            final ResponseEntity<T> exchange = restTemplate.exchange(
                    urlWithQuery, method, new HttpEntity<>(body, headersWithCookies),
                    clazz);
            final List<String> cookieStrings = exchange.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookieStrings != null) {
                loginInfo.updateCookies(cookieStrings.stream());
                wechatLoginInfoRepository.saveCookie(loginInfo);
            }
            log.info("http request {} {}", method, urlWithQuery, queryParam);
            log.debug("http response {}", exchange.getBody());
            return exchange.getBody();
        } catch (RestClientException | JsonProcessingException ex) {
            log.warn("fail http request {} {}", method, urlWithQuery, ex);
            return null;
        }
    }

    public Optional<YourWechatLoginInfo> sendRawMessage(YourWechatLoginInfo loginInfo, int msgType,
            String toUser, String content) {
        try {
            log.info("sendRawMessage start: {}", getUin(loginInfo));
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
            final long localTime = System.currentTimeMillis() / 1000;
            final ImmutableMap<String, Object> msgObject =
                    ImmutableMap.<String, Object>builder()
                            .put("Content", content)
                            .put("FromUserName", loginInfo.getWebInitResponse().getUser().getUserName())
                            .put("ToUserName", toUser)
                            .put("Type", new Integer(msgType))
                            .put("LocalID", new Long(localTime * 1000))
                            .put("ClientMsgId", new Long(localTime * 1000))
                    .build();
            final String body =
                    request(loginInfo,
                            HttpMethod.POST,
                            loginInfo.getRelativeUrl("webwxsendmsg"),
                            Collections.EMPTY_MAP,
                            headers,
                            ImmutableMap.of(
                                    "BaseRequest", loginInfo.buildBaseRequest(),
                                    "Scene", 0,
                                    "Msg", msgObject),
                            String.class);
            log.info("sendRawMessage end:  {} -> {}", getUin(loginInfo), body);
            return Optional.of(loginInfo);
        } catch (Exception ex) {
            log.warn("sendRawMessage error {}", loginInfo, ex);
        }
        return Optional.empty();
    }
}

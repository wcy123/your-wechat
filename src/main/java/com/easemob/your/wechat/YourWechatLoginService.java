package com.easemob.your.wechat;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class YourWechatLoginService {
    private final static Pattern statusCodePattern =
            Pattern.compile("window.code=(\\d+).*");
    private final static Pattern redirectUrlPattern =
            Pattern.compile("window.redirect_uri=\"(\\S+)\";.*");

    ExecutorService executorService = Executors.newSingleThreadExecutor();
    @Autowired
    WechatLoginApi loginApiWrapper;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    YourWechatLoginInfoRepository wechatLoginInfoRepository;

    public byte[] getQrImage() {
        try {
            return loginApiWrapper.getQrUuid()
                    .flatMap(this::getQrImageNext)
                    .get();
        } catch (NoSuchElementException ex) {
            throw new ResourceAccessException("cannot access wechat api");
        }
    }

    private Optional<byte[]> getQrImageNext(String code) {
        final Optional<byte[]> qrImage = loginApiWrapper.getQrImage(code);
        qrImage.ifPresent(ignore -> this.enqueue(code));
        return qrImage;
    }

    @Scheduled(fixedRate = 5000)
    public void checkQrCode() {
        String pattern = "YW:qrcode:*";
        final ScanOptions options = ScanOptions.scanOptions().match(pattern).count(10).build();

        redisTemplate.execute(new RedisCallback<Iterable<byte[]>>() {
            @Override
            public Iterable<byte[]> doInRedis(RedisConnection connection)
                    throws DataAccessException {
                List<byte[]> binaryKeys = new ArrayList<byte[]>();
                ScanOptions options = ScanOptions.scanOptions().match(pattern).count(10).build();
                try (Cursor<byte[]> cursor = connection.scan(options)) {
                    while (cursor.hasNext()) {
                        final byte[] bytes = cursor.next();
                        final int prefixLength = pattern.length() - 1;
                        final int len = bytes.length - prefixLength;
                        checkQrCode(new String(bytes, prefixLength, len, Charset.forName("UTF-8")));
                    }
                } catch (IOException e) {
                    log.error("cannot scan redis", e);
                }
                return binaryKeys;
            }
        });

    }

    private void checkQrCode(String code) {
        final String status = redisTemplate.boundValueOps(redisKey(code)).get();
        final Optional<String> loginStatus = loginApiWrapper.checkQrCode(code);
        log.info("start checking qrcode({})", code);
        loginStatus.flatMap(this::extractStatusCode)
                .flatMap(statusCode -> {
                    log.info("statusCode qrCode({}) change to status({})", code,
                            statusCode);
                    redisTemplate.boundValueOps(redisKey(code)).set(statusCode);
                    if ("200".equals(statusCode)) {
                        log.info("qrCode({}) is confirmed by user, welcome", code);
                        return loginStatus.flatMap(this::extractRedirectUrl);
                    } else if ("400".equals(statusCode)) {
                        log.info("qrCode({}) is expired", code);
                        redisTemplate.delete(redisKey(code));
                        return Optional.empty();
                    } else {
                        return Optional.empty();
                    }
                })
                .ifPresent(url -> {
                    log.info("code({}) is confirmed by user, redirect url is {}", code, url);
                    redisTemplate.delete(redisKey(code));
                    final YourWechatLoginInfo loginInfo = new YourWechatLoginInfo();
                    loginInfo.setUrl(url);
                    executorService.execute(() -> {
                        webInit(loginInfo);
                    });
                });
    }

    private void webInit(YourWechatLoginInfo loginInfo) {
        loginApiWrapper.webInitStep1(loginInfo)
                .flatMap(this::checkBaseReqeustRetCode)
                .flatMap(loginApiWrapper::webInitStep2)
                .flatMap(this::checkWebInitResponse)
                .flatMap(loginApiWrapper::showMobileLogin)
                .flatMap(loginApiWrapper::retrieveContactList)
                .flatMap(this::saveLoginInfo);

    }



    private Optional<YourWechatLoginInfo> checkBaseReqeustRetCode(YourWechatLoginInfo yourWechatLoginInfo) {
        if(yourWechatLoginInfo.getBaseResponse().getRet().equals(0)){
            return Optional.of(yourWechatLoginInfo);
        }else {
            log.error("sorry, login failed. {}", yourWechatLoginInfo);
            return Optional.empty();
        }
    }
    private Optional<YourWechatLoginInfo> checkWebInitResponse(YourWechatLoginInfo yourWechatLoginInfo) {
        if(!StringUtils.isEmpty(yourWechatLoginInfo.getWebInitResponse().getUser().getUin())){
            return Optional.of(yourWechatLoginInfo);
        }else {
            log.error("sorry, no uin found. {}", yourWechatLoginInfo);
            return Optional.empty();
        }
    }
    private  Optional<YourWechatLoginInfo> saveLoginInfo(YourWechatLoginInfo loginInfo) {
        wechatLoginInfoRepository.save(loginInfo);
        return Optional.empty();
    }

    private Optional<String> extractStatusCode(String loginStatus) {
        return extractStatusField(loginStatus, statusCodePattern);
    }

    private Optional<String> extractRedirectUrl(String loginStatus) {
        return extractStatusField(loginStatus, redirectUrlPattern);
    }

    private Optional<String> extractStatusField(String loginStatus, Pattern pattern) {
        String result = null;
        final Matcher matcher = pattern.matcher(loginStatus);
        if (matcher.find()) {
            final String statusCode = matcher.group(1);
            return Optional.of(statusCode);
        } else {
            return Optional.empty();
        }
    }

    private String enqueue(String code) {
        final BoundValueOperations<String, String> valueOps =
                redisTemplate.boundValueOps(redisKey(code));
        valueOps.set("window.code=100;");
        valueOps.expire(10, TimeUnit.MINUTES);
        return code;
    }

    private String redisKey(String code) {
        return "YW:qrcode:" + code;
    }
}

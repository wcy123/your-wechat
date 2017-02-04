package com.easemob.your.wechat;

import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.wcy123.ProtobufMessageConverter;
import org.wcy123.protobuf.your.wechat.WechatProtos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import com.google.common.collect.ImmutableList;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Configuration
@EnableScheduling
public class YourWechatConfig {
    private static final int DEFAULT_MAX_TOTAL_CONNECTIONS = 100;
    private static final int DEFAULT_MAX_CONNECTIONS_PER_ROUTE = 5;
    private static final int DEFAULT_READ_TIMEOUT_MILLISECONDS = (60 * 1000);

    @Bean
    public WechatLoginApi loginApiWrapper() {
        return new WechatLoginApi();
    }

    @Bean
    public JedisPool getPool() throws URISyntaxException {
        URI redisURI = new URI(System.getenv("REDIS_URL"));
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        // poolConfig.setmaxc(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        JedisPool pool = new JedisPool(poolConfig, redisURI);
        return pool;
    }

    @Bean
    public ClientHttpRequestFactory httpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(httpClient());
    }

    @Bean
    public RestTemplate restTemplate(ProtobufMessageConverter protobufMessageConverter) {
        RestTemplate restTemplate = new RestTemplate(httpRequestFactory());
        restTemplate.setMessageConverters(
                ImmutableList.of(jaxb2RootElementHttpMessageConverter(),
                        protobufMessageConverter,
                        mappingJackson2HttpMessageConverter(),
                        stringHttpMessageConverter(),
                        byteArrayHttpMessageConverter()));
        return restTemplate;
    }

    @Bean

    Jaxb2RootElementHttpMessageConverter jaxb2RootElementHttpMessageConverter() {
        final Jaxb2RootElementHttpMessageConverter jaxb2RootElementHttpMessageConverter =
                new Jaxb2RootElementHttpMessageConverter();
        jaxb2RootElementHttpMessageConverter
                .setSupportedMediaTypes(ImmutableList.of(MediaType.ALL));
        return jaxb2RootElementHttpMessageConverter;
    }

    @Bean
    MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        final MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setObjectMapper(objectMapper());
        return mappingJackson2HttpMessageConverter;
    }

    @Bean
    StringHttpMessageConverter stringHttpMessageConverter() {
        return new StringHttpMessageConverter();
    }

    @Bean
    ByteArrayHttpMessageConverter byteArrayHttpMessageConverter() {
        return new ByteArrayHttpMessageConverter();
    }

    @Bean
    ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
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
                .addDeserializer(WechatProtos.MemberList.class,
                        new ProtobufFieldDeserializer(WechatProtos.MemberList.class))
                .addSerializer(WechatProtos.SyncKey.class,
                        new ProtobufFieldSerializer<>())
                .addDeserializer(WechatProtos.SyncKey.class,
                        new ProtobufFieldDeserializer(WechatProtos.SyncKey.class))
                ;
        mapper.registerModule(module);
        return mapper;
    }

    @Bean
    public HttpClient httpClient() {

        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager();
        HttpClientBuilder defaultHttpClient = HttpClientBuilder.create();

        defaultHttpClient.setMaxConnTotal(DEFAULT_MAX_TOTAL_CONNECTIONS)
                .setMaxConnPerRoute(DEFAULT_MAX_CONNECTIONS_PER_ROUTE)
                .setDefaultRequestConfig(
                        RequestConfig.copy(RequestConfig.DEFAULT)
                                .setConnectionRequestTimeout(DEFAULT_READ_TIMEOUT_MILLISECONDS)
                                .build())
                .disableRedirectHandling()
                .disableCookieManagement()
                .addInterceptorLast(new HttpResponseInterceptor() {
                    public void process(
                            final HttpResponse response,
                            final HttpContext context) throws HttpException, IOException {
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            Header ceheader = entity.getContentEncoding();
                            if (ceheader != null) {
                                HeaderElement[] codecs = ceheader.getElements();
                                for (int i = 0; i < codecs.length; i++) {
                                    if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                                        response.setEntity(
                                                new GzipDecompressingEntity(response.getEntity()));
                                        return;
                                    }
                                }
                            }
                        }
                    }

                });

        final CloseableHttpClient httpClient = defaultHttpClient.build();

        return httpClient;
    }
}

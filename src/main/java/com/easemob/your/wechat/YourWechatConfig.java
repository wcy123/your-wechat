package com.easemob.your.wechat;

import java.io.IOException;

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

import com.google.common.collect.ImmutableList;

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
        return new MappingJackson2HttpMessageConverter();
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

package com.easemob.your.wechat;

import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import static org.junit.Assert.*;


@Ignore
public class ApiBaseResponseTest {
    @Test
    public void main1() throws Exception {
        System.out.println(System.getProperties());
        final byte[] bytes = Files.readAllBytes(Paths.get(System.getProperty("user.home") + "/GitHub/your-wechat/record/redirect_url.record"));
        JAXBContext jaxbContext = JAXBContext.newInstance(ApiBaseResponse.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        final Object unmarshal = (ApiBaseResponse) unmarshaller.unmarshal(new ByteArrayInputStream(bytes));
        System.out.println(unmarshal);
    }
}

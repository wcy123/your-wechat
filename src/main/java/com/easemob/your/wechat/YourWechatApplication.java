package com.easemob.your.wechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class YourWechatApplication {

	public static void main(String[] args) {
		System.setProperty("jsse.enableSNIExtension","false");
		SpringApplication.run(YourWechatApplication.class, args);
	}
}

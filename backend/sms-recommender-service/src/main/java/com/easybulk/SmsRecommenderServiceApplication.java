package com.easybulk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "com.easybulk.client")
public class SmsRecommenderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmsRecommenderServiceApplication.class, args);
	}

}

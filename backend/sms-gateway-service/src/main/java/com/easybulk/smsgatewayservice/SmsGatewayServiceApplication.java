package com.easybulk.smsgatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.easybulk.smsgatewayservice", "com.easybulk.common"})
public class SmsGatewayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmsGatewayServiceApplication.class, args);
    }
}

package com.ecold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {OAuth2ClientAutoConfiguration.class})
@EnableScheduling
@EnableAsync
public class EColdApplication {
    public static void main(String[] args) {
        SpringApplication.run(EColdApplication.class, args);
    }
}
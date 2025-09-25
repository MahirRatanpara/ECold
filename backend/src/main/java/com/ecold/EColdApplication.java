package com.ecold;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;

@SpringBootApplication(exclude = {OAuth2ClientAutoConfiguration.class})
@EnableScheduling
@EnableAsync
@EnableCaching
@EnableBatchProcessing
public class EColdApplication {
    public static void main(String[] args) {
        SpringApplication.run(EColdApplication.class, args);
    }
}
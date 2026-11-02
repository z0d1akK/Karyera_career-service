package com.zodiakk.careerservice;

import com.zodiakk.careerservice.outbox.config.OutboxProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties(OutboxProperties.class)
public class CareerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CareerServiceApplication.class, args);
    }

}

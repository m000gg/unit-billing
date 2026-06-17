package com.m000gg.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication(scanBasePackages = {"com.m000gg.client", "com.m000gg.shared"})
@EnableJpaRepositories(basePackages = {"com.m000gg.client", "com.m000gg.shared.repository"})
@EntityScan(basePackages = {"com.m000gg.client", "com.m000gg.shared.entity"})
public class ClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClientApplication.class, args);
    }

}



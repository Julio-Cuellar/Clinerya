package com.jclinical.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.jclinical")
@EnableJpaRepositories(basePackages = "com.jclinical")
@EntityScan(basePackages = "com.jclinical")
@EnableScheduling
public class ClineryaApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClineryaApplication.class, args);
    }


}
package com.chimera.learnservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class LearnServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearnServiceApplication.class, args);
    }
}

package com.chimera.actservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ActServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActServiceApplication.class, args);
    }
}

package com.voyado.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executors;

@SpringBootApplication
@EnableScheduling
public class EventGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventGeneratorApplication.class, args);
    }

    @Bean
    public java.util.concurrent.ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

package com.voyado.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;

import java.util.concurrent.Executors;

@SpringBootApplication
@EnableKafka
public class KafkaClickHouseConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaClickHouseConsumerApplication.class, args);
    }

    @Bean
    public java.util.concurrent.ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

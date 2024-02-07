package com.tagstory;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableBatchProcessing
@SpringBootApplication
public class StoryBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(StoryBatchApplication.class, args);
    }
}
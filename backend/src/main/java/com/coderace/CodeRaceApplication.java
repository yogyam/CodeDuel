package com.coderace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CodeRaceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeRaceApplication.class, args);
    }
}

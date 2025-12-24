package com.coderace.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Application configuration for beans
 * Provides centralized configuration for RestTemplate, ObjectMapper, and other
 * shared components
 */
@Configuration
@EnableRetry
public class AppConfig {

    /**
     * Creates a RestTemplate bean with proper timeout configuration
     * This enables connection pooling and centralized error handling
     * 
     * @param builder RestTemplateBuilder provided by Spring Boot
     * @return Configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(120)) // Increased for Gemini 2.5 thinking mode
                .build();
    }

    /**
     * Creates an ObjectMapper bean for JSON processing
     * Ensures consistent JSON serialization/deserialization across the application
     * 
     * @return ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}

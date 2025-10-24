package com.studyassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class - Entry point for the Spring Boot application
 * 
 * @SpringBootApplication annotation does 3 things:
 * 1. @Configuration - Allows defining beans
 * 2. @EnableAutoConfiguration - Auto-configures Spring based on dependencies
 * 3. @ComponentScan - Scans for components in this package and sub-packages
 */
@SpringBootApplication
public class StudyAssistantApplication {

    /**
     * Main method - starts the Spring Boot application
     * This creates an embedded Tomcat server and runs our REST API
     */
    public static void main(String[] args) {
        SpringApplication.run(StudyAssistantApplication.class, args);
        System.out.println("🚀 AI Study Assistant API is running on http://localhost:8080");
    }
}
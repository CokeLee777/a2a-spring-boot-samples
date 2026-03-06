package com.github.cokelee777.a2aclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the A2A Client Spring Boot application.
 * <p>
 * This application serves as an LLM-powered chat interface that routes user queries to
 * specialized microagents via the Agent-to-Agent (A2A) Protocol. The client leverages
 * Spring AI to perform intent analysis and agent orchestration.
 * </p>
 */
@SpringBootApplication
public class A2aClientApplication {

	/**
	 * Main method to start the Spring Boot application.
	 * @param args command-line arguments passed to Spring
	 */
	public static void main(String[] args) {
		SpringApplication.run(A2aClientApplication.class, args);
	}

}

package com.github.cokelee777.orderagentserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Order Agent Spring Boot application.
 * <p>
 * This agent handles order-related queries including order listing and cancellation
 * eligibility checks. It communicates with delivery and payment agents to make informed
 * decisions about order status.
 * </p>
 */
@SpringBootApplication
public class OrderAgentApplication {

	/**
	 * Main method to start the Spring Boot application.
	 * @param args command-line arguments passed to Spring
	 */
	public static void main(String[] args) {
		SpringApplication.run(OrderAgentApplication.class, args);
	}

}

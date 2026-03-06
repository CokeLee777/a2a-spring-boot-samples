package com.github.cokelee777.paymentagentserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Payment Agent Spring Boot application.
 * <p>
 * This agent handles payment-related queries including payment status lookups and refund
 * eligibility checks for orders. It communicates with the order agent via the A2A
 * Protocol to provide refund status information.
 * </p>
 */
@SpringBootApplication
public class PaymentAgentApplication {

	/**
	 * Main method to start the Spring Boot application.
	 * @param args command-line arguments passed to Spring
	 */
	public static void main(String[] args) {
		SpringApplication.run(PaymentAgentApplication.class, args);
	}

}

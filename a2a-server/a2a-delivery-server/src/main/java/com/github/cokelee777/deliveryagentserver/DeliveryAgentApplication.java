package com.github.cokelee777.deliveryagentserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Delivery Agent Spring Boot application.
 * <p>
 * This agent is responsible for providing delivery tracking information. It responds to
 * A2A Protocol requests to query shipment status and delivery details.
 * </p>
 */
@SpringBootApplication
public class DeliveryAgentApplication {

	/**
	 * Main method to start the Spring Boot application.
	 * @param args command-line arguments passed to Spring
	 */
	public static void main(String[] args) {
		SpringApplication.run(DeliveryAgentApplication.class, args);
	}

}

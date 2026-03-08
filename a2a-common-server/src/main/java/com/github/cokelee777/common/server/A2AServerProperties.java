package com.github.cokelee777.common.server;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the A2A server infrastructure.
 *
 * <p>
 * Bind values via {@code a2a.server.*} in {@code application.yml}.
 * </p>
 *
 * @param blockingAgentTimeoutSeconds timeout in seconds for blocking agent execution
 * @param blockingConsumptionTimeoutSeconds timeout in seconds for blocking event
 * consumption
 * @param executorCorePoolSize core thread pool size for the agent executor
 * @param executorMaxPoolSize maximum thread pool size for the agent executor
 * @param executorQueueCapacity task queue capacity for the agent executor
 */
@ConfigurationProperties(prefix = "a2a.server")
public record A2AServerProperties(int blockingAgentTimeoutSeconds, int blockingConsumptionTimeoutSeconds,
		int executorCorePoolSize, int executorMaxPoolSize, int executorQueueCapacity) {

	/**
	 * Default constructor with sensible defaults.
	 */
	public A2AServerProperties() {
		this(30, 30, 10, 50, 100);
	}
}

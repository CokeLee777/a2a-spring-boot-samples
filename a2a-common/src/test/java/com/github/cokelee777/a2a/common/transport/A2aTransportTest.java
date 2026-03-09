package com.github.cokelee777.a2a.common.transport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Structural tests for A2aTransport. Full integration tests require a live agent; only
 * constructibility is verified here.
 */
class A2aTransportTest {

	@Test
	void constructor_withUrl_doesNotThrow() {
		A2aTransport transport = new A2aTransport("http://localhost:9999");
		assertNotNull(transport);
	}

}

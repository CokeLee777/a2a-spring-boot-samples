package com.github.cokelee777.a2a.server;

import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.config.A2AConfigProvider;
import io.a2a.server.config.DefaultValuesConfigProvider;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.MainEventBus;
import io.a2a.server.events.MainEventBusProcessor;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.tasks.BasePushNotificationSender;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.spec.AgentCard;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Auto-configuration that assembles the core A2A server infrastructure beans.
 *
 * <p>
 * This configuration provides the following infrastructure components:
 * </p>
 * <ul>
 * <li>{@link A2AConfigProvider} — bridges Spring Environment to A2A SDK</li>
 * <li>{@link InMemoryTaskStore} — in-memory task persistence</li>
 * <li>{@link InMemoryPushNotificationConfigStore} — in-memory push notification
 * config</li>
 * <li>{@link BasePushNotificationSender} — HTTP-based push notification dispatching</li>
 * <li>{@link MainEventBus} — internal event bus for agent lifecycle events</li>
 * <li>{@link InMemoryQueueManager} — per-task event queue management</li>
 * <li>{@link MainEventBusProcessor} — background processor started via
 * {@link SmartLifecycle}</li>
 * <li>{@link ExecutorService} beans — thread pools for agent execution and event
 * consumption</li>
 * <li>{@link DefaultRequestHandler} — main JSON-RPC request handler wiring all
 * components</li>
 * </ul>
 *
 * <p>
 * Consumer modules only need to provide an {@link AgentExecutor} bean and include this
 * module on the classpath; all other infrastructure is wired automatically.
 * </p>
 */
@AutoConfiguration
@EnableConfigurationProperties(A2AServerProperties.class)
public class A2AServerAutoConfiguration {

	/**
	 * Registers the A2A Agent Card controller as a bean so it's discoverable by Spring
	 * even if the server module's component scanning doesn't reach this package.
	 * @param agentCard the agent card bean provided by the server module
	 * @return a configured {@link A2AAgentCardController}
	 */
	@Bean
	public A2AAgentCardController a2aAgentCardController(AgentCard agentCard) {
		return new A2AAgentCardController(agentCard);
	}

	/**
	 * Registers the A2A JSON-RPC controller as a bean so it's discoverable by Spring even
	 * if the server module's component scanning doesn't reach this package.
	 * @param requestHandler the request handler managing all JSON-RPC request processing
	 * @return a configured {@link A2AJsonRpcController}
	 */
	@Bean
	public A2AJsonRpcController a2aJsonRpcController(DefaultRequestHandler requestHandler) {
		return new A2AJsonRpcController(requestHandler);
	}

	/**
	 * Creates the A2A SDK configuration provider using default values.
	 *
	 * <p>
	 * This provider bridges the Spring Environment to the A2A SDK, making any custom
	 * configuration available to SDK components that need it.
	 * </p>
	 * @return a configured {@link A2AConfigProvider} instance
	 */
	@Bean
	public A2AConfigProvider configProvider() {
		return new DefaultValuesConfigProvider();
	}

	/**
	 * Creates the in-memory task store used to persist task state during agent execution.
	 * @return a new {@link InMemoryTaskStore} instance
	 */
	@Bean
	public InMemoryTaskStore taskStore() {
		return new InMemoryTaskStore();
	}

	/**
	 * Creates the in-memory store for push notification configurations.
	 * @return a new {@link InMemoryPushNotificationConfigStore} instance
	 */
	@Bean
	public PushNotificationConfigStore pushNotificationConfigStore() {
		return new InMemoryPushNotificationConfigStore();
	}

	/**
	 * Creates the push notification sender that dispatches HTTP callbacks when tasks
	 * complete.
	 * @param pushNotificationConfigStore the store holding push notification endpoint
	 * configs
	 * @return a new {@link BasePushNotificationSender} backed by the default HTTP client
	 */
	@Bean
	public BasePushNotificationSender pushNotificationSender(PushNotificationConfigStore pushNotificationConfigStore) {
		return new BasePushNotificationSender(pushNotificationConfigStore, A2AHttpClientFactory.create());
	}

	/**
	 * Creates the main event bus through which agent lifecycle events are published.
	 * @return a new {@link MainEventBus} instance
	 */
	@Bean
	public MainEventBus mainEventBus() {
		return new MainEventBus();
	}

	/**
	 * Creates the queue manager that maintains per-task event queues.
	 * @param taskStore the task store used as a
	 * {@link io.a2a.server.tasks.TaskStateProvider}
	 * @param mainEventBus the event bus that receives queued events
	 * @return a new {@link InMemoryQueueManager} instance
	 */
	@Bean
	public InMemoryQueueManager queueManager(InMemoryTaskStore taskStore, MainEventBus mainEventBus) {
		return new InMemoryQueueManager(taskStore, mainEventBus);
	}

	/**
	 * Creates the {@link MainEventBusProcessor} that consumes events from the main event
	 * bus and updates task state and dispatches push notifications accordingly.
	 * @param mainEventBus the event bus to consume from
	 * @param taskStore the task store updated as events arrive
	 * @param pushNotificationSender the sender invoked for terminal events
	 * @param queueManager the queue manager supplying per-task queues
	 * @return a configured {@link MainEventBusProcessor}
	 */
	@Bean
	public MainEventBusProcessor mainEventBusProcessor(MainEventBus mainEventBus, InMemoryTaskStore taskStore,
			BasePushNotificationSender pushNotificationSender, InMemoryQueueManager queueManager) {
		return new MainEventBusProcessor(mainEventBus, taskStore, pushNotificationSender, queueManager);
	}

	/**
	 * Wraps {@link MainEventBusProcessor} in a {@link SmartLifecycle} bean so that the
	 * background processor thread is started when the application context starts.
	 *
	 * <p>
	 * Stopping is handled automatically by JVM shutdown. The {@code stop()} method is a
	 * no-op because the processor's stop method is package-private and not externally
	 * accessible.
	 * </p>
	 * @param processor the event bus processor to start
	 * @return a {@link SmartLifecycle} that calls {@code ensureStarted()} on context
	 * refresh
	 */
	@Bean
	public SmartLifecycle mainEventBusProcessorLifecycle(MainEventBusProcessor processor) {
		return new SmartLifecycle() {
			@Override
			public void start() {
				processor.ensureStarted();
			}

			@Override
			public void stop() {
				// MainEventBusProcessor.stop() is package-private; shutdown handled by
				// JVM
			}

			@Override
			public boolean isRunning() {
				return true;
			}
		};
	}

	/**
	 * Creates the primary thread pool used to execute agent logic asynchronously.
	 *
	 * <p>
	 * Thread pool parameters are sourced from {@link A2AServerProperties}.
	 * </p>
	 * @param props the server configuration properties
	 * @return a bounded {@link ThreadPoolExecutor} with caller-runs overflow policy
	 */
	@Bean(destroyMethod = "shutdown")
	public ExecutorService agentExecutorService(A2AServerProperties props) {
		return new ThreadPoolExecutor(props.executorCorePoolSize(), props.executorMaxPoolSize(), 60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(props.executorQueueCapacity()), new ThreadPoolExecutor.CallerRunsPolicy());
	}

	/**
	 * Creates the secondary thread pool used for consuming events from per-task queues.
	 *
	 * <p>
	 * Thread pool parameters are sourced from {@link A2AServerProperties}.
	 * </p>
	 * @param props the server configuration properties
	 * @return a bounded {@link ThreadPoolExecutor} with caller-runs overflow policy
	 */
	@Bean(name = "eventConsumerExecutorService", destroyMethod = "shutdown")
	public ExecutorService eventConsumerExecutorService(A2AServerProperties props) {
		return new ThreadPoolExecutor(props.executorCorePoolSize(), props.executorMaxPoolSize(), 60L, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(props.executorQueueCapacity()), new ThreadPoolExecutor.CallerRunsPolicy());
	}

	/**
	 * Creates the {@link DefaultRequestHandler} that handles all A2A JSON-RPC requests.
	 *
	 * <p>
	 * Wires together the agent executor, task store, queue manager, push notification
	 * config, event bus processor, and the two thread pools into a single request
	 * handler.
	 * </p>
	 * @param agentExecutor the application-provided agent executor bean
	 * @param taskStore the in-memory task store
	 * @param queueManager the in-memory queue manager
	 * @param pushNotificationConfigStore the push notification config store
	 * @param processor the event bus processor managing task event lifecycle
	 * @param agentExecutorService the thread pool for agent execution
	 * @param eventConsumerExecutorService the thread pool for event consumption
	 * @return a configured {@link DefaultRequestHandler} ready to process requests
	 */
	@Bean
	@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
	public DefaultRequestHandler requestHandler(AgentExecutor agentExecutor, InMemoryTaskStore taskStore,
			InMemoryQueueManager queueManager, PushNotificationConfigStore pushNotificationConfigStore,
			MainEventBusProcessor processor, ExecutorService agentExecutorService,
			ExecutorService eventConsumerExecutorService) {
		return DefaultRequestHandler.create(agentExecutor, taskStore, queueManager, pushNotificationConfigStore,
				processor, agentExecutorService, eventConsumerExecutorService);
	}

}

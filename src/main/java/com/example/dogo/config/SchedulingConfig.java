package com.example.dogo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Dedicated {@link TaskScheduler} for {@code @Scheduled} batch jobs.
 *
 * <p>Without this bean the only {@link TaskScheduler} in the context is the WebSocket
 * broker's {@code messageBrokerTaskScheduler} (registered by {@code @EnableWebSocketMessageBroker}),
 * so all {@code @Scheduled} tasks would run on the {@code MessageBroker-*} threads meant for
 * STOMP messaging. Defining a bean explicitly named {@code taskScheduler} makes the scheduler
 * resolver pick it instead, isolating batch work from real-time messaging.
 */
@Configuration
public class SchedulingConfig {

	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(4);
		scheduler.setThreadNamePrefix("scheduled-");
		scheduler.setWaitForTasksToCompleteOnShutdown(true);
		scheduler.setAwaitTerminationSeconds(30);
		return scheduler;
	}
}

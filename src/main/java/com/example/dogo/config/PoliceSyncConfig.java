package com.example.dogo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class PoliceSyncConfig {

	@Bean
	public TaskExecutor policeStartupBackfillExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(3);
		executor.setMaxPoolSize(3);
		executor.setQueueCapacity(0);
		executor.setThreadNamePrefix("police-backfill-");
		executor.initialize();
		return executor;
	}

	@Bean
	public TaskExecutor itemMatchTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("item-match-");
		executor.initialize();
		return executor;
	}
}

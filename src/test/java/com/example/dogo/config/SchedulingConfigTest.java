package com.example.dogo.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulingConfigTest {

	@Test
	void scheduledTasksRunOnDedicatedPoolNotMessageBrokerPool() throws Exception {
		try (AnnotationConfigApplicationContext context =
				new AnnotationConfigApplicationContext(TestSchedulingContext.class)) {

			ProbeBean probe = context.getBean(ProbeBean.class);
			assertThat(probe.latch.await(5, TimeUnit.SECONDS))
					.as("@Scheduled method should have executed")
					.isTrue();

			assertThat(probe.threadName.get())
					.as("batch should run on dedicated scheduler, not the WebSocket broker pool")
					.startsWith("scheduled-")
					.doesNotContain("MessageBroker");
		}
	}

	@Configuration
	@EnableScheduling
	static class TestSchedulingContext {

		// Mimics the bean registered by @EnableWebSocketMessageBroker.
		@Bean
		public TaskScheduler messageBrokerTaskScheduler() {
			ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
			scheduler.setThreadNamePrefix("MessageBroker-");
			scheduler.initialize();
			return scheduler;
		}

		@Bean
		public TaskScheduler taskScheduler() {
			return new SchedulingConfig().taskScheduler();
		}

		@Bean
		public ProbeBean probeBean() {
			return new ProbeBean();
		}
	}

	static class ProbeBean {
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicReference<String> threadName = new AtomicReference<>();

		@Scheduled(fixedDelay = 60_000)
		public void run() {
			if (latch.getCount() > 0) {
				threadName.set(Thread.currentThread().getName());
				latch.countDown();
			}
		}
	}
}

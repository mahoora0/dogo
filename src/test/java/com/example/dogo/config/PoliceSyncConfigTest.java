package com.example.dogo.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

class PoliceSyncConfigTest {

	private ThreadPoolTaskExecutor executor;

	@AfterEach
	void tearDown() {
		if (executor != null) {
			executor.shutdown();
		}
	}

	@Test
	void policeStartupBackfillExecutorAcceptsThreeStartupTasks() throws Exception {
		executor = (ThreadPoolTaskExecutor) new PoliceSyncConfig().policeStartupBackfillExecutor();
		CountDownLatch firstTwoTasksStarted = new CountDownLatch(2);
		CountDownLatch releaseTasks = new CountDownLatch(1);

		executor.execute(blockingTask(firstTwoTasksStarted, releaseTasks));
		executor.execute(blockingTask(firstTwoTasksStarted, releaseTasks));

		if (!firstTwoTasksStarted.await(Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS)) {
			throw new AssertionError("Timed out waiting for initial startup tasks to occupy the executor");
		}

		assertThatCode(() -> executor.execute(blockingTask(new CountDownLatch(0), releaseTasks)))
				.doesNotThrowAnyException();

		releaseTasks.countDown();
	}

	private Runnable blockingTask(CountDownLatch started, CountDownLatch release) {
		return () -> {
			started.countDown();
			try {
				release.await();
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
			}
		};
	}
}

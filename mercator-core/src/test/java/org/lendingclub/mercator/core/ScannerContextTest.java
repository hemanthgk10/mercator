package org.lendingclub.mercator.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.lendingclub.mercator.core.ScannerContext.CleanupTask;

public class ScannerContextTest {

	@Test
	public void testNoContext() {

		Assertions.assertThat(ScannerContext.getScannerContext().isPresent()).isFalse();
	}

	@Test
	public void testContext() {

		Assertions.assertThat(ScannerContext.getScannerContext().isPresent()).isFalse();

		new ScannerContext().withName("test").exec(ctx -> {
			Assertions.assertThat(ctx.getEntityCount()).isEqualTo(0);
			Assertions.assertThat(ScannerContext.getScannerContext().get()).isSameAs(ctx);
		});

		Assertions.assertThat(ScannerContext.getScannerContext().isPresent()).isFalse();

	}

	@Test
	public void testExceptionThrownFromContext() {
		Assertions.assertThat(ScannerContext.getScannerContext().isPresent()).isFalse();


		try {
			new ScannerContext().exec(ctx -> {
				throw new RuntimeException("simulated failure");
			});
		} catch (RuntimeException e) {
			Assertions.assertThat(e).hasMessageContaining("simulated failure");
		}

		Assertions.assertThat(ScannerContext.getScannerContext().isPresent()).isFalse();

	}

	@Test
	public void testNestedContext() {
		Assertions.assertThat(ScannerContext.getScannerContext().isPresent()).isFalse();

		new ScannerContext().withName("test").exec(ctx -> {
			Assertions.assertThat(ctx.getEntityCount()).isEqualTo(0);
			Assertions.assertThat(ScannerContext.getScannerContext().get()).isSameAs(ctx);

			new ScannerContext().withName("inner").exec(innerCtx -> {
				Assertions.assertThat(ctx.getEntityCount()).isEqualTo(0);
				Assertions.assertThat(ScannerContext.getScannerContext().get()).isSameAs(innerCtx);

			});

			Assertions.assertThat(ctx.getEntityCount()).isEqualTo(0);
			Assertions.assertThat(ScannerContext.getScannerContext().get()).isSameAs(ctx);
		});

		Assertions.assertThat(ScannerContext.getScannerContext().isPresent()).isFalse();

	}

	@Test
	public void testCleanup() throws InterruptedException {

		CountDownLatch latch = new CountDownLatch(1);
		CleanupTask task = new CleanupTask() {

			@Override
			public void cleanup(ScannerContext context) {
				latch.countDown();

			}
		};
		new ScannerContext().withName("test").withCleanupTask(task).exec(ctx -> {
			Assertions.assertThat(ctx.getMap().get("name")).isEqualTo("test");
		});

		Assertions.assertThat(latch.await(1, TimeUnit.SECONDS)).isTrue();

	}
}

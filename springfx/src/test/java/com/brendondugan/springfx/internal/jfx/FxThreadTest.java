package com.brendondugan.springfx.internal.jfx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/** Runs FxThread against a stand-in for the JavaFX application thread, so no toolkit starts. */
class FxThreadTest {

    private final AtomicReference<Thread> standInThread = new AtomicReference<>();
    private final ExecutorService standIn = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "stand-in FX thread");
        standInThread.set(thread);
        return thread;
    });
    private final FxThread fxThread = new FxThread(() -> Thread.currentThread() == standInThread.get(), standIn);

    @AfterEach
    void stopStandIn() {
        standIn.shutdownNow();
    }

    @Test
    void callFromAnotherThreadRunsOnTheFxThreadAndReturnsItsResult() {
        assertThat(fxThread.call(() -> Thread.currentThread().getName())).isEqualTo("stand-in FX thread");
    }

    @Test
    void callOnTheFxThreadRunsInline() throws Exception {
        String result = CompletableFuture.supplyAsync(() -> fxThread.call(() -> fxThread.call(() -> "nested")), standIn)
                .get(5, TimeUnit.SECONDS);

        assertThat(result).isEqualTo("nested");
    }

    @Test
    void callRethrowsTheWorksExceptionUnwrapped() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> fxThread.call(() -> {
                    throw new IllegalArgumentException("boom");
                }))
                .withMessage("boom");
    }

    @Test
    void runFromAnotherThreadDoesNotWait() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch ran = new CountDownLatch(1);

        fxThread.run(() -> {
            awaitQuietly(release);
            ran.countDown();
        });

        assertThat(ran.getCount()).isEqualTo(1);
        release.countDown();
        assertThat(ran.await(5, TimeUnit.SECONDS)).isTrue();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

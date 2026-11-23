package com.brendondugan.springfx.internal.jfx;

import javafx.application.Platform;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/** Runs work on the JavaFX application thread, from whichever thread asks. */
class FxThread {

    private final BooleanSupplier onFxThread;
    private final Executor fxExecutor;

    /** The real JavaFX application thread. Creating this doesn't start the toolkit. */
    FxThread() {
        this(Platform::isFxApplicationThread, Platform::runLater);
    }

    FxThread(BooleanSupplier onFxThread, Executor fxExecutor) {
        this.onFxThread = onFxThread;
        this.fxExecutor = fxExecutor;
    }

    /** Runs the work on the FX thread and returns its result, waiting for it if called from elsewhere. */
    <T> T call(Supplier<T> work) {
        if (onFxThread.getAsBoolean()) {
            return work.get();
        }
        FutureTask<T> task = new FutureTask<>(work::get);
        fxExecutor.execute(task);
        try {
            return task.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for the JavaFX application thread", e);
        } catch (CancellationException e) {
            throw new IllegalStateException("Work on the JavaFX application thread was cancelled", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(e.getCause());
        }
    }

    /** Runs the work on the FX thread: now if called on it, else later without waiting. */
    void run(Runnable work) {
        if (onFxThread.getAsBoolean()) {
            work.run();
        } else {
            fxExecutor.execute(work);
        }
    }
}

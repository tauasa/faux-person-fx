package org.tauasa.faux.app;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javafx.application.Platform;

/** Starts the JavaFX toolkit once per JVM and runs test code on its thread. Needs a display. */
final class Fx {

    private static volatile boolean started;

    private Fx() {
    }

    static synchronized void start() throws InterruptedException {
        if (started) {
            return;
        }
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyStarted) {
            ready.countDown();
        }
        if (!ready.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("JavaFX didn't start");
        }
        Platform.setImplicitExit(false);
        started = true;
    }

    static <T> T call(Supplier<T> work) throws Exception {
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(work.get());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });
        return result.get(10, TimeUnit.SECONDS);
    }

    static void run(Runnable work) throws Exception {
        call(() -> {
            work.run();
            return null;
        });
    }
}

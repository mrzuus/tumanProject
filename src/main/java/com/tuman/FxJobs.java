package com.tuman;

import javafx.application.Platform;
import javafx.scene.Node;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Serializes disk operations away from the JavaFX application thread. */
public final class FxJobs {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "cash-storage");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicInteger PENDING = new AtomicInteger();
    private static volatile CompletableFuture<Void> last = CompletableFuture.completedFuture(null);

    private FxJobs() { }

    public static <T> void run(Node controls, Callable<T> action, Consumer<T> success) {
        if (!Platform.isFxApplicationThread()) throw new IllegalStateException("UI thread required");
        controls.setDisable(true);
        PENDING.incrementAndGet();
        CompletableFuture<Void> completion = new CompletableFuture<>();
        last = completion;
        WORKER.submit(() -> {
            T value = null;
            Exception failure = null;
            try { value = action.call(); } catch (Exception e) { failure = e; }
            T result = value;
            Exception error = failure;
            Platform.runLater(() -> {
                try {
                    controls.setDisable(false);
                    if (error == null) success.accept(result);
                    else mainController.showError(error);
                    completion.complete(null);
                } catch (Exception e) {
                    completion.completeExceptionally(e);
                } finally { PENDING.decrementAndGet(); }
            });
        });
    }

    public static boolean isBusy() { return PENDING.get() > 0; }
    public static CompletableFuture<Void> whenIdle() { return last; }

    public static void stop() throws InterruptedException {
        WORKER.shutdown();
        if (!WORKER.awaitTermination(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Запись данных ещё не завершена.");
        }
    }
}

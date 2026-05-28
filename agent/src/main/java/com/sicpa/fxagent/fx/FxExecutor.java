package com.sicpa.fxagent.fx;

import javafx.application.Platform;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public final class FxExecutor {

    private FxExecutor() {}

    public static <T> CompletableFuture<T> supplyOnFxThread(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        if (Platform.isFxApplicationThread()) {
            try {
                future.complete(supplier.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } else {
            Platform.runLater(() -> {
                try {
                    future.complete(supplier.get());
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }
        return future;
    }

    public static CompletableFuture<Void> runOnFxThread(Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (Platform.isFxApplicationThread()) {
            try {
                runnable.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } else {
            Platform.runLater(() -> {
                try {
                    runnable.run();
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
        }
        return future;
    }

    public static <T> T supplyOnFxThreadBlocking(Supplier<T> supplier, long timeoutMs) {
        try {
            return supplyOnFxThread(supplier).get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new RuntimeException("FX thread operation timed out after " + timeoutMs + "ms", e);
        } catch (Exception e) {
            throw new RuntimeException("FX thread operation failed", e);
        }
    }
}

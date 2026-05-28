package com.sicpa.fxagent.fx;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class FxToolkitWaiter {

    private static final Logger log = LoggerFactory.getLogger(FxToolkitWaiter.class);
    private static final long POLL_INTERVAL_MS = 100;

    private FxToolkitWaiter() {}

    public static void awaitToolkitReady(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        int attempts = 0;

        while (System.currentTimeMillis() < deadline) {
            try {
                CompletableFuture<Void> probe = new CompletableFuture<>();
                Platform.runLater(() -> probe.complete(null));
                probe.get(Math.min(1000, deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
                log.debug("JavaFX toolkit ready after {} attempts", attempts + 1);
                return;
            } catch (IllegalStateException e) {
                attempts++;
                if (attempts % 50 == 0) {
                    log.debug("Still waiting for JavaFX toolkit ({} attempts)...", attempts);
                }
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for JavaFX toolkit", ie);
                }
            } catch (Exception e) {
                attempts++;
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for JavaFX toolkit", ie);
                }
            }
        }

        throw new RuntimeException("JavaFX toolkit did not become ready within " + timeoutMs + "ms");
    }
}

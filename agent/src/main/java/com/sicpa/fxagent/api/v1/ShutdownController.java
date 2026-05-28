package com.sicpa.fxagent.api.v1;

import io.javalin.http.Context;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ShutdownController {

    private static final Logger log = LoggerFactory.getLogger(ShutdownController.class);
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "fxagent-shutdown");
                t.setDaemon(true);
                return t;
            });

    public void shutdown(Context ctx) {
        log.info("Shutdown requested via HTTP");
        ctx.status(200).json(Map.of("status", "shutting-down"));

        // Schedule on a separate thread so the response can be sent first,
        // then terminate the JavaFX runtime and JVM with a small delay
        // so the HTTP response has time to flush.
        scheduler.schedule(this::terminate, 100, TimeUnit.MILLISECONDS);
    }

    private void terminate() {
        try {
            Platform.exit();
        } catch (Throwable t) {
            log.warn("Platform.exit() failed", t);
        }
        // Force JVM exit after a brief grace period — Platform.exit() may not
        // unblock the JVM if other non-daemon threads are running.
        scheduler.schedule(() -> System.exit(0), 300, TimeUnit.MILLISECONDS);
    }
}

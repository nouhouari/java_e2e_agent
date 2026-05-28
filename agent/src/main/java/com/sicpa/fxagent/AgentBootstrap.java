package com.sicpa.fxagent;

import com.sicpa.fxagent.fx.FxToolkitWaiter;
import com.sicpa.fxagent.server.AgentServer;
import com.sicpa.fxagent.util.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public final class AgentBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AgentBootstrap.class);
    private static final AtomicBoolean started = new AtomicBoolean(false);
    private static volatile AgentServer server;

    private AgentBootstrap() {}

    public static void start(String agentArgs, FxAgent.AttachMode mode) {
        if (!started.compareAndSet(false, true)) {
            log.warn("FxAgent already started, ignoring duplicate attach");
            return;
        }

        AgentConfig config = AgentConfig.parse(agentArgs);
        log.info("FxAgent starting (mode={}, port={}, toolkitTimeout={}ms)",
                mode, config.port(), config.toolkitTimeout());

        Thread daemon = new Thread(() -> bootstrap(config), "fxagent-bootstrap");
        daemon.setDaemon(true);
        daemon.start();
    }

    private static void bootstrap(AgentConfig config) {
        try {
            log.info("Waiting for JavaFX toolkit...");
            FxToolkitWaiter.awaitToolkitReady(config.toolkitTimeout());
            log.info("JavaFX toolkit is ready");

            server = new AgentServer(config);
            server.start();

            System.out.println("FXAGENT_READY port=" + server.port());
            System.out.flush();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("FxAgent shutting down");
                if (server != null) {
                    server.stop();
                }
            }, "fxagent-shutdown"));

        } catch (Exception e) {
            log.error("FxAgent bootstrap failed", e);
            started.set(false);
        }
    }
}

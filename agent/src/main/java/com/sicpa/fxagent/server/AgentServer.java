package com.sicpa.fxagent.server;

import com.sicpa.fxagent.action.ActionExecutor;
import com.sicpa.fxagent.api.v1.ActionController;
import com.sicpa.fxagent.api.v1.ElementController;
import com.sicpa.fxagent.api.v1.HealthController;
import com.sicpa.fxagent.api.v1.ScreenshotController;
import com.sicpa.fxagent.api.v1.WindowController;
import com.sicpa.fxagent.util.AgentConfig;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AgentServer {

    private static final Logger log = LoggerFactory.getLogger(AgentServer.class);
    private final AgentConfig config;
    private Javalin app;

    public AgentServer(AgentConfig config) {
        this.config = config;
    }

    public void start() {
        var healthController = new HealthController();
        var windowController = new WindowController();
        var elementController = new ElementController();
        var screenshotController = new ScreenshotController();
        var actionExecutor = new ActionExecutor();
        var actionController = new ActionController(actionExecutor);

        app = Javalin.create(cfg -> {
            cfg.jsonMapper(JsonMapperFactory.create());
            cfg.showJavalinBanner = false;
        });

        ErrorHandler.register(app);

        app.get("/api/v1/health", healthController::health);
        app.get("/api/v1/ready", healthController::ready);
        app.get("/api/v1/windows", windowController::list);
        app.get("/api/v1/windows/{index}", windowController::get);
        app.post("/api/v1/elements/query", elementController::query);
        app.get("/api/v1/elements/{handle}", elementController::get);
        app.get("/api/v1/elements/{handle}/tree", elementController::tree);
        app.post("/api/v1/elements/wait", elementController::waitFor);
        app.get("/api/v1/scene/tree", elementController::fullTree);
        app.post("/api/v1/screenshot", screenshotController::capture);
        app.post("/api/v1/actions", actionController::execute);

        app.start(config.port());
        log.info("AgentServer started on port {}", config.port());
    }

    public void stop() {
        if (app != null) {
            app.stop();
            log.info("AgentServer stopped");
        }
    }

    public int port() {
        return app != null ? app.port() : config.port();
    }
}

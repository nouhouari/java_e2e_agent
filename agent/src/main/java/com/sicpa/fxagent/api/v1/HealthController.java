package com.sicpa.fxagent.api.v1;

import com.sicpa.fxagent.fx.FxExecutor;
import com.sicpa.fxagent.util.AgentVersion;
import io.javalin.http.Context;
import javafx.stage.Window;

import java.util.Map;

public final class HealthController {

    public void health(Context ctx) {
        ctx.json(Map.of("status", "ok", "version", AgentVersion.get()));
    }

    public void ready(Context ctx) {
        ctx.future(() -> FxExecutor.supplyOnFxThread(() -> Window.getWindows().size())
                .thenAccept(windowCount -> {
                    if (windowCount > 0) {
                        ctx.json(Map.of("status", "ready", "windowCount", windowCount));
                    } else {
                        ctx.status(503).json(Map.of("status", "not_ready", "windowCount", 0));
                    }
                }));
    }
}

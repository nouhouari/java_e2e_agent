package com.sicpa.fxagent.api.v1;

import com.sicpa.fxagent.action.ActionExecutor;
import com.sicpa.fxagent.api.dto.ActionRequest;
import io.javalin.http.Context;

public final class ActionController {

    private final ActionExecutor executor;

    public ActionController(ActionExecutor executor) {
        this.executor = executor;
    }

    public void execute(Context ctx) {
        ActionRequest request = ctx.bodyAsClass(ActionRequest.class);
        var future = executor.execute(request);
        ctx.future(() -> future.thenAccept(ctx::json));
    }
}

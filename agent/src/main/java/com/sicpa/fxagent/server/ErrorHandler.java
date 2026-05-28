package com.sicpa.fxagent.server;

import com.sicpa.fxagent.action.NodeNotFoundException;
import com.sicpa.fxagent.action.UnsupportedActionException;
import com.sicpa.fxagent.api.dto.ErrorResponse;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

public final class ErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandler.class);

    private ErrorHandler() {}

    public static void register(Javalin app) {
        app.exception(NodeNotFoundException.class, (e, ctx) -> {
            log.debug("Node not found: {}", e.getMessage());
            ctx.status(404).json(new ErrorResponse("NODE_NOT_FOUND", e.getMessage()));
        });

        app.exception(UnsupportedActionException.class, (e, ctx) -> {
            log.debug("Invalid action: {}", e.getMessage());
            ctx.status(400).json(new ErrorResponse("INVALID_ACTION", e.getMessage()));
        });

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            log.debug("Bad request: {}", e.getMessage());
            ctx.status(400).json(new ErrorResponse("BAD_REQUEST", e.getMessage()));
        });

        app.exception(TimeoutException.class, (e, ctx) -> {
            log.debug("Timeout: {}", e.getMessage());
            ctx.status(408).json(new ErrorResponse("TIMEOUT", e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            log.error("Unhandled exception", e);
            ctx.status(500).json(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
        });
    }
}

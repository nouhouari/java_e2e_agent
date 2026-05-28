package com.sicpa.fxagent.action;

import com.sicpa.fxagent.api.dto.ActionRequest;
import com.sicpa.fxagent.api.dto.ActionResponse;
import com.sicpa.fxagent.api.dto.ElementDTO;
import com.sicpa.fxagent.fx.FxExecutor;
import com.sicpa.fxagent.scene.NodeRegistry;
import com.sicpa.fxagent.scene.NodeSerializer;
import com.sicpa.fxagent.scene.selector.SelectorEngine;
import com.sicpa.fxagent.scene.selector.SelectorParser;
import javafx.application.Platform;
import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class ActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(ActionExecutor.class);

    // Click-family actions are fire-and-forget: dispatch the click event on
    // the FX thread, then return immediately. This prevents a deadlock when
    // the user's onAction handler calls Alert.showAndWait(), which blocks the
    // FX thread in a nested event loop. Subsequent agent requests (e.g. the
    // OK click that dismisses the dialog) get drained by that nested loop.
    private static final Set<String> FIRE_AND_FORGET_ACTIONS =
            Set.of("click", "dblclick", "rightclick", "hover");

    private final ClickAction clickAction = new ClickAction();
    private final FillAction fillAction = new FillAction();
    private final SelectAction selectAction = new SelectAction();
    private final FocusAction focusAction = new FocusAction();
    private final ScrollAction scrollAction = new ScrollAction();
    private final SetTextAction setTextAction = new SetTextAction();

    public CompletableFuture<ActionResponse> execute(ActionRequest request) {
        if (FIRE_AND_FORGET_ACTIONS.contains(request.action())) {
            return executeFireAndForget(request);
        }
        return executeSync(request);
    }

    private CompletableFuture<ActionResponse> executeFireAndForget(ActionRequest request) {
        // Resolve the node synchronously so "not found" / "invalid selector"
        // errors are reported to the client. This call is fast and does not
        // invoke any user code, so it cannot block on showAndWait.
        Node node;
        try {
            node = FxExecutor.supplyOnFxThreadBlocking(() -> resolveNode(request), 3000);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return CompletableFuture.failedFuture(cause);
        }

        // Dispatch the click without awaiting its completion. Any exception
        // thrown by the user's handler is logged but not propagated to the
        // client (the response was already sent).
        Platform.runLater(() -> {
            try {
                ActionResponse result = dispatchClick(request.action(), node, request);
                if (!result.success()) {
                    log.warn("Click action '{}' failed after dispatch: {}",
                            request.action(), result.message());
                }
            } catch (Throwable t) {
                log.warn("Click action '{}' threw after dispatch", request.action(), t);
            }
        });

        return CompletableFuture.completedFuture(
                ActionResponse.success("Dispatched " + request.action()));
    }

    private CompletableFuture<ActionResponse> executeSync(ActionRequest request) {
        return FxExecutor.supplyOnFxThread(() -> {
            Node node = resolveNode(request);

            ActionResponse result = switch (request.action()) {
                case "fill" -> fillAction.execute(node, request);
                case "clear" -> fillAction.clear(node);
                case "select" -> selectAction.execute(node, request);
                case "focus" -> focusAction.execute(node);
                case "scroll" -> scrollAction.execute(node);
                case "setText" -> setTextAction.execute(node, request);
                default -> throw new UnsupportedActionException("Unknown action: " + request.action());
            };

            if (result.success()) {
                ElementDTO updated = NodeSerializer.serializeShallow(node);
                return ActionResponse.success(result.message(), updated);
            }
            return result;
        });
    }

    private ActionResponse dispatchClick(String action, Node node, ActionRequest request) {
        return switch (action) {
            case "click" -> clickAction.execute(node, request);
            case "dblclick" -> clickAction.doubleClick(node, request);
            case "rightclick" -> clickAction.rightClick(node, request);
            case "hover" -> clickAction.hover(node, request);
            default -> throw new UnsupportedActionException("Unknown click action: " + action);
        };
    }

    private Node resolveNode(ActionRequest request) {
        if (request.handle() != null && !request.handle().isBlank()) {
            return NodeRegistry.instance().resolve(request.handle())
                    .orElseThrow(() -> new NodeNotFoundException("Node not found: " + request.handle()));
        }
        if (request.selector() != null && !request.selector().isBlank()) {
            var parsed = SelectorParser.parse(request.selector());
            return SelectorEngine.queryFirst(parsed)
                    .orElseThrow(() -> new NodeNotFoundException("No node matches: " + request.selector()));
        }
        throw new IllegalArgumentException("Either handle or selector must be provided");
    }
}

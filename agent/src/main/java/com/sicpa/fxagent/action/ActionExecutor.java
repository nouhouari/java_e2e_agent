package com.sicpa.fxagent.action;

import com.sicpa.fxagent.api.dto.ActionRequest;
import com.sicpa.fxagent.api.dto.ActionResponse;
import com.sicpa.fxagent.api.dto.ElementDTO;
import com.sicpa.fxagent.fx.FxExecutor;
import com.sicpa.fxagent.scene.NodeRegistry;
import com.sicpa.fxagent.scene.NodeSerializer;
import com.sicpa.fxagent.scene.selector.SelectorEngine;
import com.sicpa.fxagent.scene.selector.SelectorParser;
import javafx.scene.Node;

import java.util.concurrent.CompletableFuture;

public final class ActionExecutor {

    private final ClickAction clickAction = new ClickAction();
    private final FillAction fillAction = new FillAction();
    private final SelectAction selectAction = new SelectAction();
    private final FocusAction focusAction = new FocusAction();
    private final ScrollAction scrollAction = new ScrollAction();

    public CompletableFuture<ActionResponse> execute(ActionRequest request) {
        return FxExecutor.supplyOnFxThread(() -> {
            Node node = resolveNode(request);

            ActionResponse result = switch (request.action()) {
                case "click" -> clickAction.execute(node, request);
                case "dblclick" -> clickAction.doubleClick(node, request);
                case "rightclick" -> clickAction.rightClick(node, request);
                case "hover" -> clickAction.hover(node, request);
                case "fill" -> fillAction.execute(node, request);
                case "clear" -> fillAction.clear(node);
                case "select" -> selectAction.execute(node, request);
                case "focus" -> focusAction.execute(node);
                case "scroll" -> scrollAction.execute(node);
                default -> throw new UnsupportedActionException("Unknown action: " + request.action());
            };

            // Include updated element state in response
            if (result.success()) {
                ElementDTO updated = NodeSerializer.serializeShallow(node);
                return ActionResponse.success(result.message(), updated);
            }
            return result;
        });
    }

    private Node resolveNode(ActionRequest request) {
        // Try handle first
        if (request.handle() != null && !request.handle().isBlank()) {
            return NodeRegistry.instance().resolve(request.handle())
                    .orElseThrow(() -> new NodeNotFoundException("Node not found: " + request.handle()));
        }
        // Fall back to selector
        if (request.selector() != null && !request.selector().isBlank()) {
            var parsed = SelectorParser.parse(request.selector());
            return SelectorEngine.queryFirst(parsed)
                    .orElseThrow(() -> new NodeNotFoundException("No node matches: " + request.selector()));
        }
        throw new IllegalArgumentException("Either handle or selector must be provided");
    }
}

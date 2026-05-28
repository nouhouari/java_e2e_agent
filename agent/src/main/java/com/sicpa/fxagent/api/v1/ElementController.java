package com.sicpa.fxagent.api.v1;

import com.sicpa.fxagent.action.NodeNotFoundException;
import com.sicpa.fxagent.api.dto.ElementDTO;
import com.sicpa.fxagent.api.dto.QueryRequest;
import com.sicpa.fxagent.api.dto.QueryResponse;
import com.sicpa.fxagent.api.dto.WaitRequest;
import com.sicpa.fxagent.fx.FxExecutor;
import com.sicpa.fxagent.scene.NodeRegistry;
import com.sicpa.fxagent.scene.NodeSerializer;
import com.sicpa.fxagent.scene.SceneGraphWalker;
import com.sicpa.fxagent.scene.selector.ChainedSelector;
import com.sicpa.fxagent.scene.selector.SelectorEngine;
import com.sicpa.fxagent.scene.selector.SelectorParser;
import io.javalin.http.Context;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public final class ElementController {

    public void query(Context ctx) {
        QueryRequest request = ctx.bodyAsClass(QueryRequest.class);
        if (request.selector() == null || request.selector().isBlank()) {
            throw new IllegalArgumentException("selector is required");
        }

        ChainedSelector parsed = SelectorParser.parse(request.selector());
        int maxResults = request.maxResults();

        ctx.future(() -> FxExecutor.supplyOnFxThread(() -> {
            List<Node> nodes;
            if (request.windowIndex() != null) {
                List<Window> windows = List.copyOf(Window.getWindows());
                int idx = request.windowIndex();
                if (idx < 0 || idx >= windows.size()) {
                    throw new IllegalArgumentException(
                            "Window index %d out of range (0..%d)".formatted(idx, windows.size() - 1));
                }
                nodes = SelectorEngine.queryAllInWindow(windows.get(idx), parsed);
            } else {
                nodes = SelectorEngine.queryAll(parsed);
            }

            List<ElementDTO> elements = nodes.stream()
                    .limit(maxResults)
                    .map(NodeSerializer::serializeShallow)
                    .toList();

            return new QueryResponse(elements, elements.size());
        }).thenAccept(ctx::json));
    }

    public void get(Context ctx) {
        String handle = ctx.pathParam("handle");
        ctx.future(() -> FxExecutor.supplyOnFxThread(() -> {
            Node node = NodeRegistry.instance().resolve(handle)
                    .orElseThrow(() -> new NodeNotFoundException("No node found for handle: " + handle));
            return NodeSerializer.serializeShallow(node);
        }).thenAccept(ctx::json));
    }

    public void tree(Context ctx) {
        String handle = ctx.pathParam("handle");
        int depth = clampDepth(ctx.queryParam("depth"), 3, 10);

        ctx.future(() -> FxExecutor.supplyOnFxThread(() -> {
            Node node = NodeRegistry.instance().resolve(handle)
                    .orElseThrow(() -> new NodeNotFoundException("No node found for handle: " + handle));
            return NodeSerializer.serialize(node, depth);
        }).thenAccept(ctx::json));
    }

    public void waitFor(Context ctx) {
        WaitRequest request = ctx.bodyAsClass(WaitRequest.class);
        if (request.selector() == null || request.selector().isBlank()) {
            throw new IllegalArgumentException("selector is required");
        }

        String condition = request.condition() != null ? request.condition() : "exists";
        long timeoutMs = request.timeoutMs() > 0 ? request.timeoutMs() : 5000;
        long pollIntervalMs = request.pollIntervalMs() > 0 ? request.pollIntervalMs() : 200;

        ChainedSelector parsed = SelectorParser.parse(request.selector());
        long deadline = System.currentTimeMillis() + timeoutMs;

        CompletableFuture<ElementDTO> future = new CompletableFuture<>();

        Thread poller = new Thread(() -> {
            try {
                while (System.currentTimeMillis() < deadline) {
                    ElementDTO match = FxExecutor.supplyOnFxThread(() -> {
                        List<Node> nodes = SelectorEngine.queryAll(parsed);
                        if (nodes.isEmpty()) return null;
                        Node node = nodes.getFirst();
                        return NodeSerializer.serializeShallow(node);
                    }).get();

                    if (matchesCondition(match, condition)) {
                        future.complete(match);
                        return;
                    }
                    Thread.sleep(pollIntervalMs);
                }
                future.completeExceptionally(new TimeoutException(
                        "Timed out waiting for '%s' with condition '%s' after %dms"
                                .formatted(request.selector(), condition, timeoutMs)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                future.completeExceptionally(e);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }, "fxagent-wait-poller");
        poller.setDaemon(true);
        poller.start();

        ctx.future(() -> future.thenAccept(ctx::json));
    }

    public void fullTree(Context ctx) {
        int depth = clampDepth(ctx.queryParam("depth"), 5, 10);

        ctx.future(() -> FxExecutor.supplyOnFxThread(() -> {
            List<Window> windows = SceneGraphWalker.getWindows();
            List<Map<String, Object>> windowTrees = new ArrayList<>();

            for (int i = 0; i < windows.size(); i++) {
                Window window = windows.get(i);
                Scene scene = window.getScene();
                ElementDTO rootDto = null;
                if (scene != null && scene.getRoot() != null) {
                    rootDto = NodeSerializer.serialize(scene.getRoot(), depth);
                }
                windowTrees.add(Map.of(
                        "windowIndex", i,
                        "windowType", window.getClass().getSimpleName(),
                        "root", rootDto != null ? rootDto : Map.of()
                ));
            }
            return Map.of("windows", windowTrees);
        }).thenAccept(ctx::json));
    }

    private static boolean matchesCondition(ElementDTO element, String condition) {
        if (element == null) {
            return "hidden".equals(condition);
        }
        return switch (condition) {
            case "exists" -> true;
            case "visible" -> element.visible();
            case "enabled" -> element.enabled();
            case "hidden" -> false;
            default -> throw new IllegalArgumentException("Unknown condition: " + condition);
        };
    }

    private static int clampDepth(String depthParam, int defaultValue, int maxValue) {
        if (depthParam == null) return defaultValue;
        try {
            int d = Integer.parseInt(depthParam);
            return Math.max(0, Math.min(d, maxValue));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}

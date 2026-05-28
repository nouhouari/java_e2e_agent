package com.sicpa.fxagent.api.v1;

import com.sicpa.fxagent.action.NodeNotFoundException;
import com.sicpa.fxagent.api.dto.ScreenshotRequest;
import com.sicpa.fxagent.fx.FxExecutor;
import com.sicpa.fxagent.scene.NodeRegistry;
import com.sicpa.fxagent.scene.selector.SelectorEngine;
import com.sicpa.fxagent.scene.selector.SelectorParser;
import io.javalin.http.Context;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public final class ScreenshotController {

    public void capture(Context ctx) {
        ScreenshotRequest request = ctx.bodyAsClass(ScreenshotRequest.class);

        ctx.future(() -> FxExecutor.supplyOnFxThread(() -> {
            WritableImage image;

            if (request.handle() != null && !request.handle().isBlank()) {
                Node node = NodeRegistry.instance().resolve(request.handle())
                        .orElseThrow(() -> new NodeNotFoundException("Node not found: " + request.handle()));
                image = snapshotNode(node);
            } else if (request.selector() != null && !request.selector().isBlank()) {
                var parsed = SelectorParser.parse(request.selector());
                Node node = SelectorEngine.queryFirst(parsed)
                        .orElseThrow(() -> new NodeNotFoundException("No node matches: " + request.selector()));
                image = snapshotNode(node);
            } else {
                Scene scene = resolveScene(request.windowIndex());
                image = scene.snapshot(null);
            }

            int width = (int) image.getWidth();
            int height = (int) image.getHeight();
            BufferedImage buffered = SwingFXUtils.fromFXImage(image, null);
            String base64 = Base64.getEncoder().encodeToString(toPngBytes(buffered));

            return Map.of(
                    "data", base64,
                    "format", "png",
                    "width", width,
                    "height", height
            );
        }).thenAccept(ctx::json));
    }

    private static WritableImage snapshotNode(Node node) {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        return node.snapshot(params, null);
    }

    private static Scene resolveScene(Integer windowIndex) {
        List<Window> windows = List.copyOf(Window.getWindows());
        if (windows.isEmpty()) {
            throw new IllegalStateException("No windows available for screenshot");
        }

        int idx = windowIndex != null ? windowIndex : 0;
        if (idx < 0 || idx >= windows.size()) {
            throw new IllegalArgumentException("Window index " + idx + " out of range (0-" + (windows.size() - 1) + ")");
        }

        Scene scene = windows.get(idx).getScene();
        if (scene == null) {
            throw new IllegalStateException("Window " + idx + " has no scene");
        }
        return scene;
    }

    private static byte[] toPngBytes(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode screenshot as PNG", e);
        }
    }
}

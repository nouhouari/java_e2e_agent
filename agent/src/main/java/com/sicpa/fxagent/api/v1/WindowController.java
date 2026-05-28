package com.sicpa.fxagent.api.v1;

import com.sicpa.fxagent.api.dto.WindowDTO;
import com.sicpa.fxagent.fx.FxExecutor;
import io.javalin.http.Context;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WindowController {

    public void list(Context ctx) {
        ctx.future(() -> FxExecutor.supplyOnFxThread(() -> {
            List<Window> windows = List.copyOf(Window.getWindows());
            List<WindowDTO> dtos = new ArrayList<>();
            for (int i = 0; i < windows.size(); i++) {
                dtos.add(toDTO(windows.get(i), i));
            }
            return dtos;
        }).thenAccept(dtos -> ctx.json(Map.of("windows", dtos))));
    }

    public void get(Context ctx) {
        int index = Integer.parseInt(ctx.pathParam("index"));
        ctx.future(() -> FxExecutor.supplyOnFxThread(() -> {
            List<Window> windows = List.copyOf(Window.getWindows());
            if (index < 0 || index >= windows.size()) {
                throw new IllegalArgumentException(
                        "Window index %d out of range (0..%d)".formatted(index, windows.size() - 1));
            }
            return toDTO(windows.get(index), index);
        }).thenAccept(ctx::json));
    }

    private static WindowDTO toDTO(Window window, int index) {
        String title = window instanceof Stage stage ? stage.getTitle() : null;
        String type = window.getClass().getSimpleName();
        return new WindowDTO(
                index,
                title,
                type,
                window.getX(),
                window.getY(),
                window.getWidth(),
                window.getHeight(),
                window.isFocused(),
                window.isShowing()
        );
    }
}

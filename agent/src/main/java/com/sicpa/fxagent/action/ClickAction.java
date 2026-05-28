package com.sicpa.fxagent.action;

import com.sicpa.fxagent.api.dto.ActionRequest;
import com.sicpa.fxagent.api.dto.ActionResponse;
import com.sicpa.fxagent.fx.RobotProvider;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.robot.Robot;

public final class ClickAction {

    public ActionResponse execute(Node node, ActionRequest request) {
        return doClick(node, request, 1, MouseButton.PRIMARY);
    }

    public ActionResponse doubleClick(Node node, ActionRequest request) {
        return doClick(node, request, 2, MouseButton.PRIMARY);
    }

    public ActionResponse rightClick(Node node, ActionRequest request) {
        return doClick(node, request, 1, MouseButton.SECONDARY);
    }

    public ActionResponse hover(Node node, ActionRequest request) {
        try {
            Point2D screen = screenCenter(node);
            if (screen == null) {
                return ActionResponse.error("Cannot determine screen coordinates for node");
            }
            Robot robot = RobotProvider.get();
            robot.mouseMove(screen.getX(), screen.getY());
            return ActionResponse.success("Hovered at (" + screen.getX() + ", " + screen.getY() + ")");
        } catch (Exception e) {
            return ActionResponse.error("Hover failed: " + e.getMessage());
        }
    }

    private ActionResponse doClick(Node node, ActionRequest request, int clickCount, MouseButton button) {
        try {
            String strategy = request.option("strategy");

            if ("robot".equals(strategy)) {
                return robotClick(node, button, clickCount);
            }

            // Tier 1: ButtonBase.fire() — most reliable for buttons (single primary click only)
            if (node instanceof ButtonBase buttonBase
                    && clickCount == 1
                    && button == MouseButton.PRIMARY) {
                buttonBase.fire();
                return ActionResponse.success("Clicked (fire) " + node.getClass().getSimpleName());
            }

            // Tier 2: Synthetic MouseEvent sequence
            return syntheticClick(node, button, clickCount);
        } catch (Exception e) {
            return ActionResponse.error("Click failed: " + e.getMessage());
        }
    }

    private ActionResponse syntheticClick(Node node, MouseButton button, int clickCount) {
        Bounds local = node.getBoundsInLocal();
        double localX = (local.getMinX() + local.getMaxX()) / 2.0;
        double localY = (local.getMinY() + local.getMaxY()) / 2.0;

        Point2D screen = node.localToScreen(localX, localY);
        double screenX = screen != null ? screen.getX() : localX;
        double screenY = screen != null ? screen.getY() : localY;

        for (int i = 0; i < clickCount; i++) {
            node.fireEvent(createMouseEvent(MouseEvent.MOUSE_PRESSED,
                    localX, localY, screenX, screenY, button, i + 1));
            node.fireEvent(createMouseEvent(MouseEvent.MOUSE_RELEASED,
                    localX, localY, screenX, screenY, button, i + 1));
            node.fireEvent(createMouseEvent(MouseEvent.MOUSE_CLICKED,
                    localX, localY, screenX, screenY, button, i + 1));
        }

        String desc = clickCount > 1 ? "Double-clicked" : "Clicked";
        if (button == MouseButton.SECONDARY) {
            desc = "Right-clicked";
        }
        return ActionResponse.success(desc + " (synthetic) " + node.getClass().getSimpleName());
    }

    private ActionResponse robotClick(Node node, MouseButton button, int clickCount) {
        Point2D screen = screenCenter(node);
        if (screen == null) {
            return ActionResponse.error("Cannot determine screen coordinates for robot click");
        }
        Robot robot = RobotProvider.get();
        robot.mouseMove(screen.getX(), screen.getY());
        for (int i = 0; i < clickCount; i++) {
            robot.mouseClick(button);
        }

        String desc = clickCount > 1 ? "Double-clicked" : "Clicked";
        if (button == MouseButton.SECONDARY) {
            desc = "Right-clicked";
        }
        return ActionResponse.success(desc + " (robot) " + node.getClass().getSimpleName());
    }

    private Point2D screenCenter(Node node) {
        Bounds local = node.getBoundsInLocal();
        double localX = (local.getMinX() + local.getMaxX()) / 2.0;
        double localY = (local.getMinY() + local.getMaxY()) / 2.0;
        return node.localToScreen(localX, localY);
    }

    private MouseEvent createMouseEvent(
            javafx.event.EventType<MouseEvent> type,
            double localX, double localY,
            double screenX, double screenY,
            MouseButton button, int clickCount) {
        return new MouseEvent(
                type,
                localX, localY,       // x, y (local)
                screenX, screenY,     // screenX, screenY
                button,
                clickCount,
                false, false, false, false, // shift, ctrl, alt, meta
                button == MouseButton.PRIMARY,   // primaryButtonDown
                false,                           // middleButtonDown
                button == MouseButton.SECONDARY, // secondaryButtonDown
                false,                           // synthesized
                false,                           // popupTrigger
                true,                            // stillSincePress
                null                             // pickResult
        );
    }
}

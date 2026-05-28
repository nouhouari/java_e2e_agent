package com.sicpa.fxagent.action;

import com.sicpa.fxagent.api.dto.ActionResponse;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;

public final class ScrollAction {

    public ActionResponse execute(Node node) {
        try {
            ScrollPane scrollPane = findAncestorScrollPane(node);
            if (scrollPane == null) {
                // Best-effort: just focus the node
                node.requestFocus();
                return ActionResponse.success("No ScrollPane found; focused node as best-effort");
            }

            scrollToNode(scrollPane, node);
            return ActionResponse.success("Scrolled to " + node.getClass().getSimpleName());
        } catch (Exception e) {
            return ActionResponse.error("Scroll failed: " + e.getMessage());
        }
    }

    private ScrollPane findAncestorScrollPane(Node node) {
        Parent parent = node.getParent();
        while (parent != null) {
            if (parent instanceof ScrollPane scrollPane) {
                return scrollPane;
            }
            parent = parent.getParent();
        }
        return null;
    }

    private void scrollToNode(ScrollPane scrollPane, Node node) {
        Node content = scrollPane.getContent();
        if (content == null) {
            return;
        }

        Bounds contentBounds = content.getBoundsInLocal();
        Bounds nodeBounds = node.getBoundsInParent();

        // Walk up from node to the content node, accumulating the transform
        Node current = node;
        double nodeMinY = nodeBounds.getMinY();
        double nodeMinX = nodeBounds.getMinX();
        double nodeMaxY = nodeBounds.getMaxY();
        double nodeMaxX = nodeBounds.getMaxX();

        // Convert node bounds to content-local coordinates
        while (current.getParent() != null && current.getParent() != content) {
            Bounds parentBounds = current.getParent().getBoundsInParent();
            // Simplified: use localToScene then sceneToLocal
            current = current.getParent();
        }

        // Calculate relative position within content
        Bounds nodeInContent = content.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
        if (nodeInContent == null) {
            return;
        }

        double contentHeight = contentBounds.getHeight();
        double contentWidth = contentBounds.getWidth();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        double viewportWidth = scrollPane.getViewportBounds().getWidth();

        // Calculate vertical scroll value
        if (contentHeight > viewportHeight) {
            double scrollableHeight = contentHeight - viewportHeight;
            double targetY = nodeInContent.getMinY() - (viewportHeight - nodeInContent.getHeight()) / 2.0;
            double vValue = Math.max(0, Math.min(1, targetY / scrollableHeight));
            scrollPane.setVvalue(vValue);
        }

        // Calculate horizontal scroll value
        if (contentWidth > viewportWidth) {
            double scrollableWidth = contentWidth - viewportWidth;
            double targetX = nodeInContent.getMinX() - (viewportWidth - nodeInContent.getWidth()) / 2.0;
            double hValue = Math.max(0, Math.min(1, targetX / scrollableWidth));
            scrollPane.setHvalue(hValue);
        }
    }
}

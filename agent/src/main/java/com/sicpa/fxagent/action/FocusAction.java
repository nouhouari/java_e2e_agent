package com.sicpa.fxagent.action;

import com.sicpa.fxagent.api.dto.ActionResponse;
import javafx.scene.Node;

public final class FocusAction {

    public ActionResponse execute(Node node) {
        try {
            node.requestFocus();
            return ActionResponse.success("Focused " + node.getClass().getSimpleName());
        } catch (Exception e) {
            return ActionResponse.error("Focus failed: " + e.getMessage());
        }
    }
}

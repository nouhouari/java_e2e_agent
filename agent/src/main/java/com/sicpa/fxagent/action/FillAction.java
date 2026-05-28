package com.sicpa.fxagent.action;

import com.sicpa.fxagent.api.dto.ActionRequest;
import com.sicpa.fxagent.api.dto.ActionResponse;
import javafx.scene.Node;
import javafx.scene.control.TextInputControl;

public final class FillAction {

    public ActionResponse execute(Node node, ActionRequest request) {
        try {
            if (!(node instanceof TextInputControl tic)) {
                return ActionResponse.error("Node is not a text input control: " + node.getClass().getSimpleName());
            }
            if (!tic.isEditable()) {
                return ActionResponse.error("Text input is not editable");
            }
            String value = request.value();
            if (value == null) {
                return ActionResponse.error("No value provided for fill action");
            }
            tic.requestFocus();
            tic.clear();
            tic.setText(value);
            tic.positionCaret(value.length());
            return ActionResponse.success("Filled with: " + value);
        } catch (Exception e) {
            return ActionResponse.error("Fill failed: " + e.getMessage());
        }
    }

    public ActionResponse clear(Node node) {
        try {
            if (!(node instanceof TextInputControl tic)) {
                return ActionResponse.error("Node is not a text input control: " + node.getClass().getSimpleName());
            }
            tic.clear();
            return ActionResponse.success("Cleared text input");
        } catch (Exception e) {
            return ActionResponse.error("Clear failed: " + e.getMessage());
        }
    }
}

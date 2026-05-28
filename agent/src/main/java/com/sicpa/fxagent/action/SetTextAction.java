package com.sicpa.fxagent.action;

import com.sicpa.fxagent.api.dto.ActionRequest;
import com.sicpa.fxagent.api.dto.ActionResponse;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;

public final class SetTextAction {

    public ActionResponse execute(Node node, ActionRequest request) {
        try {
            String value = request.value();
            if (value == null) {
                return ActionResponse.error("No value provided for setText action");
            }

            if (node instanceof Labeled labeled) {
                labeled.setText(value);
                return ActionResponse.success("Set text on " + node.getClass().getSimpleName());
            }
            if (node instanceof TextInputControl textInput) {
                textInput.setText(value);
                return ActionResponse.success("Set text on " + node.getClass().getSimpleName());
            }

            return ActionResponse.error("Node does not support setText: " + node.getClass().getSimpleName());
        } catch (Exception e) {
            return ActionResponse.error("setText failed: " + e.getMessage());
        }
    }
}

package com.sicpa.fxagent.action;

import com.sicpa.fxagent.api.dto.ActionRequest;
import com.sicpa.fxagent.api.dto.ActionResponse;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;

public final class SelectAction {

    public ActionResponse execute(Node node, ActionRequest request) {
        try {
            String value = request.value();
            if (value == null) {
                return ActionResponse.error("No value provided for select action");
            }

            if (node instanceof ComboBox<?> comboBox) {
                return selectInComboBox(comboBox, value);
            }
            if (node instanceof ChoiceBox<?> choiceBox) {
                return selectInChoiceBox(choiceBox, value);
            }

            return ActionResponse.error("Node is not a selection control: " + node.getClass().getSimpleName());
        } catch (Exception e) {
            return ActionResponse.error("Select failed: " + e.getMessage());
        }
    }

    private ActionResponse selectInComboBox(ComboBox<?> comboBox, String value) {
        var items = comboBox.getItems();
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (item != null && item.toString().equals(value)) {
                comboBox.getSelectionModel().select(i);
                return ActionResponse.success("Selected '" + value + "' in ComboBox");
            }
        }
        return ActionResponse.error("Value '" + value + "' not found in ComboBox items");
    }

    private ActionResponse selectInChoiceBox(ChoiceBox<?> choiceBox, String value) {
        var items = choiceBox.getItems();
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (item != null && item.toString().equals(value)) {
                choiceBox.getSelectionModel().select(i);
                return ActionResponse.success("Selected '" + value + "' in ChoiceBox");
            }
        }
        return ActionResponse.error("Value '" + value + "' not found in ChoiceBox items");
    }
}

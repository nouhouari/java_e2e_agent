package com.sicpa.fxagent.action;

import com.sicpa.fxagent.api.dto.ActionRequest;
import com.sicpa.fxagent.api.dto.ActionResponse;
import javafx.scene.Node;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;

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
            if (node instanceof ListView<?> listView) {
                return selectInListView(listView, value);
            }
            if (node instanceof TableView<?> tableView) {
                return selectInTableView(tableView, value);
            }
            if (node instanceof ColorPicker colorPicker) {
                return selectInColorPicker(colorPicker, value);
            }

            return ActionResponse.error("Node is not a selection control: " + node.getClass().getSimpleName());
        } catch (Exception e) {
            return ActionResponse.error("Select failed: " + e.getMessage());
        }
    }

    private ActionResponse selectInColorPicker(ColorPicker colorPicker, String value) {
        Color color;
        try {
            color = Color.web(value);
        } catch (IllegalArgumentException e) {
            return ActionResponse.error("Invalid color value '" + value + "': " + e.getMessage());
        }
        colorPicker.setValue(color);
        return ActionResponse.success("Set ColorPicker to " + value);
    }

    private ActionResponse selectInListView(ListView<?> listView, String value) {
        // Allow index-based selection: "index=2"
        if (value.startsWith("index=")) {
            int idx = Integer.parseInt(value.substring(6));
            listView.getSelectionModel().select(idx);
            listView.scrollTo(idx);
            return ActionResponse.success("Selected index " + idx + " in ListView");
        }
        var items = listView.getItems();
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (item != null && item.toString().equals(value)) {
                listView.getSelectionModel().select(i);
                listView.scrollTo(i);
                return ActionResponse.success("Selected '" + value + "' in ListView");
            }
        }
        return ActionResponse.error("Value '" + value + "' not found in ListView items");
    }

    private ActionResponse selectInTableView(TableView<?> tableView, String value) {
        if (value.startsWith("index=")) {
            int idx = Integer.parseInt(value.substring(6));
            tableView.getSelectionModel().select(idx);
            tableView.scrollTo(idx);
            return ActionResponse.success("Selected row " + idx + " in TableView");
        }
        var items = tableView.getItems();
        for (int i = 0; i < items.size(); i++) {
            Object item = items.get(i);
            if (item == null) continue;
            // Match by toString() OR by any column value containing the search string
            if (item.toString().equals(value) || rowContainsValue(tableView, item, value)) {
                tableView.getSelectionModel().select(i);
                tableView.scrollTo(i);
                return ActionResponse.success("Selected row " + i + " ('" + value + "') in TableView");
            }
        }
        return ActionResponse.error("Value '" + value + "' not found in TableView rows");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean rowContainsValue(TableView<?> tableView, Object item, String value) {
        for (var col : tableView.getColumns()) {
            try {
                var cellData = ((javafx.scene.control.TableColumn) col).getCellData(item);
                if (cellData != null && cellData.toString().equals(value)) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
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

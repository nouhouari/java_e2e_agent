package com.sicpa.fxagent.scene;

import com.sicpa.fxagent.api.dto.BoundsDTO;
import com.sicpa.fxagent.api.dto.ElementDTO;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NodeSerializer {

    private NodeSerializer() {}

    public static ElementDTO serialize(Node node, int maxDepth) {
        return serializeNode(node, maxDepth, 0);
    }

    public static ElementDTO serializeShallow(Node node) {
        return serializeNode(node, 0, 0);
    }

    private static ElementDTO serializeNode(Node node, int maxDepth, int currentDepth) {
        String handle = NodeRegistry.instance().getHandle(node);
        String id = node.getId();
        String type = node.getClass().getSimpleName();
        String fullType = node.getClass().getName();
        List<String> styleClasses = List.copyOf(node.getStyleClass());
        String text = extractText(node);
        BoundsDTO bounds = extractScreenBounds(node);
        boolean visible = node.isVisible() && !node.isDisabled();
        boolean enabled = !node.isDisabled();
        boolean focused = node.isFocused();
        Map<String, Object> properties = extractProperties(node);

        List<ElementDTO> children = null;
        if (currentDepth < maxDepth && node instanceof Parent parent) {
            children = new ArrayList<>();
            for (Node child : parent.getChildrenUnmodifiable()) {
                children.add(serializeNode(child, maxDepth, currentDepth + 1));
            }
        }

        return new ElementDTO(handle, id, type, fullType, styleClasses, text,
                bounds, visible, enabled, focused, properties, children);
    }

    private static String extractText(Node node) {
        return switch (node) {
            case Labeled labeled -> labeled.getText();
            case TextInputControl tic -> tic.getText();
            default -> null;
        };
    }

    private static BoundsDTO extractScreenBounds(Node node) {
        try {
            Bounds localBounds = node.getBoundsInLocal();
            Bounds screenBounds = node.localToScreen(localBounds);
            if (screenBounds == null) return null;
            return new BoundsDTO(
                    screenBounds.getMinX(),
                    screenBounds.getMinY(),
                    screenBounds.getWidth(),
                    screenBounds.getHeight()
            );
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractProperties(Node node) {
        Map<String, Object> props = new LinkedHashMap<>();

        switch (node) {
            case CheckBox cb -> {
                props.put("selected", cb.isSelected());
                props.put("indeterminate", cb.isIndeterminate());
            }
            case RadioButton rb -> props.put("selected", rb.isSelected());
            case ToggleButton tb -> props.put("selected", tb.isSelected());
            case ComboBox<?> combo -> {
                Object value = combo.getValue();
                props.put("value", value != null ? value.toString() : null);
                props.put("editable", combo.isEditable());
            }
            case ChoiceBox<?> choice -> {
                Object value = choice.getValue();
                props.put("value", value != null ? value.toString() : null);
            }
            case Slider slider -> {
                props.put("value", slider.getValue());
                props.put("min", slider.getMin());
                props.put("max", slider.getMax());
            }
            case ProgressBar pb -> props.put("progress", pb.getProgress());
            case ProgressIndicator pi -> props.put("progress", pi.getProgress());
            case TextInputControl tic -> {
                props.put("editable", tic.isEditable());
                props.put("promptText", tic.getPromptText());
            }
            case TabPane tabPane -> {
                int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();
                props.put("selectedTabIndex", selectedIndex);
                props.put("tabCount", tabPane.getTabs().size());
            }
            case ListView<?> listView -> {
                int selectedIndex = listView.getSelectionModel().getSelectedIndex();
                props.put("selectedIndex", selectedIndex);
                props.put("itemCount", listView.getItems().size());
            }
            case TableView<?> tableView -> {
                int selectedIndex = tableView.getSelectionModel().getSelectedIndex();
                props.put("selectedIndex", selectedIndex);
                props.put("rowCount", tableView.getItems().size());
                props.put("columnCount", tableView.getColumns().size());
            }
            default -> {}
        }

        return props.isEmpty() ? null : props;
    }
}

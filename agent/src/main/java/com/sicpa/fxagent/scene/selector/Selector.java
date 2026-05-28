package com.sicpa.fxagent.scene.selector;

import com.sicpa.fxagent.scene.NodeRegistry;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.control.TextInputControl;

import java.util.List;

public sealed interface Selector {

    boolean matches(Node node);

    record Id(String id) implements Selector {
        public boolean matches(Node node) { return id.equals(node.getId()); }
    }

    record StyleClass(String className) implements Selector {
        public boolean matches(Node node) { return node.getStyleClass().contains(className); }
    }

    record TypeName(String typeName) implements Selector {
        public boolean matches(Node node) {
            return node.getClass().getSimpleName().equals(typeName)
                || node.getClass().getName().equals(typeName);
        }
    }

    record Text(String text, boolean exact) implements Selector {
        public boolean matches(Node node) {
            String nodeText = extractText(node);
            if (nodeText == null) return false;
            return exact ? text.equals(nodeText) : nodeText.contains(text);
        }

        private static String extractText(Node node) {
            return switch (node) {
                case Labeled labeled -> labeled.getText();
                case TextInputControl tic -> tic.getText();
                default -> null;
            };
        }
    }

    record Css(String cssSelector) implements Selector {
        public boolean matches(Node node) {
            throw new UnsupportedOperationException("CSS selector matching uses Node.lookupAll()");
        }
    }

    record Handle(String handle) implements Selector {
        public boolean matches(Node node) {
            return handle.equals(NodeRegistry.instance().getHandle(node));
        }
    }

    record Compound(List<Selector> selectors) implements Selector {
        public boolean matches(Node node) {
            return selectors.stream().allMatch(s -> s.matches(node));
        }
    }
}

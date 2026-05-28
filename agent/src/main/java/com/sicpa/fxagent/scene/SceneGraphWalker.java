package com.sicpa.fxagent.scene;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class SceneGraphWalker {

    private SceneGraphWalker() {}

    public static List<Window> getWindows() {
        return List.copyOf(Window.getWindows());
    }

    public static List<Node> findAll(Predicate<Node> predicate) {
        List<Node> results = new ArrayList<>();
        for (Window window : Window.getWindows()) {
            Scene scene = window.getScene();
            if (scene != null && scene.getRoot() != null) {
                walkTree(scene.getRoot(), predicate, results);
            }
        }
        return results;
    }

    public static List<Node> findAllInWindow(Window window, Predicate<Node> predicate) {
        List<Node> results = new ArrayList<>();
        Scene scene = window.getScene();
        if (scene != null && scene.getRoot() != null) {
            walkTree(scene.getRoot(), predicate, results);
        }
        return results;
    }

    public static List<Node> findAllInSubtree(Node root, Predicate<Node> predicate) {
        List<Node> results = new ArrayList<>();
        walkTree(root, predicate, results);
        return results;
    }

    private static void walkTree(Node node, Predicate<Node> predicate, List<Node> results) {
        if (predicate.test(node)) {
            results.add(node);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                walkTree(child, predicate, results);
            }
        }
    }
}

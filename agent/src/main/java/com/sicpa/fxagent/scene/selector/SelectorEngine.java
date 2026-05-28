package com.sicpa.fxagent.scene.selector;

import com.sicpa.fxagent.scene.SceneGraphWalker;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class SelectorEngine {

    private SelectorEngine() {}

    public static List<Node> queryAll(ChainedSelector chained) {
        if (chained.isSingleSelector()) {
            return resolveSelector(chained.first());
        }
        return resolveChain(chained.chain());
    }

    public static Optional<Node> queryFirst(ChainedSelector chained) {
        List<Node> all = queryAll(chained);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.getFirst());
    }

    public static List<Node> queryAllInWindow(Window window, ChainedSelector chained) {
        if (chained.isSingleSelector()) {
            return resolveSelectorInWindow(window, chained.first());
        }
        return resolveChainInWindow(window, chained.chain());
    }

    private static List<Node> resolveSelector(Selector selector) {
        if (selector instanceof Selector.Css css) {
            return resolveCss(css.cssSelector());
        }
        return SceneGraphWalker.findAll(selector::matches);
    }

    private static List<Node> resolveSelectorInWindow(Window window, Selector selector) {
        if (selector instanceof Selector.Css css) {
            return resolveCssInWindow(window, css.cssSelector());
        }
        return SceneGraphWalker.findAllInWindow(window, selector::matches);
    }

    private static List<Node> resolveSelectorInSubtree(Node root, Selector selector) {
        if (selector instanceof Selector.Css css) {
            Set<Node> found = root.lookupAll(css.cssSelector());
            return new ArrayList<>(found);
        }
        return SceneGraphWalker.findAllInSubtree(root, selector::matches);
    }

    private static List<Node> resolveChain(List<Selector> chain) {
        List<Node> candidates = resolveSelector(chain.getFirst());
        for (int i = 1; i < chain.size(); i++) {
            Selector next = chain.get(i);
            List<Node> nextCandidates = new ArrayList<>();
            for (Node parent : candidates) {
                nextCandidates.addAll(resolveSelectorInSubtree(parent, next));
            }
            candidates = nextCandidates;
        }
        return candidates;
    }

    private static List<Node> resolveChainInWindow(Window window, List<Selector> chain) {
        List<Node> candidates = resolveSelectorInWindow(window, chain.getFirst());
        for (int i = 1; i < chain.size(); i++) {
            Selector next = chain.get(i);
            List<Node> nextCandidates = new ArrayList<>();
            for (Node parent : candidates) {
                nextCandidates.addAll(resolveSelectorInSubtree(parent, next));
            }
            candidates = nextCandidates;
        }
        return candidates;
    }

    private static List<Node> resolveCss(String cssSelector) {
        List<Node> results = new ArrayList<>();
        for (Window window : SceneGraphWalker.getWindows()) {
            results.addAll(resolveCssInWindow(window, cssSelector));
        }
        return results;
    }

    private static List<Node> resolveCssInWindow(Window window, String cssSelector) {
        Scene scene = window.getScene();
        if (scene == null || scene.getRoot() == null) {
            return List.of();
        }
        Set<Node> found = scene.getRoot().lookupAll(cssSelector);
        return new ArrayList<>(found);
    }
}

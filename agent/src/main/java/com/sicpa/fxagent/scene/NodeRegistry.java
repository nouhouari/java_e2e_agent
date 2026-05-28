package com.sicpa.fxagent.scene;

import javafx.scene.Node;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class NodeRegistry {

    private static final NodeRegistry INSTANCE = new NodeRegistry();

    private final AtomicLong counter = new AtomicLong(0);
    private final Map<String, WeakReference<Node>> handleToNode = new ConcurrentHashMap<>();
    private final Map<Integer, String> identityToHandle = new ConcurrentHashMap<>();

    public static NodeRegistry instance() { return INSTANCE; }

    public String getHandle(Node node) {
        int identity = System.identityHashCode(node);
        return identityToHandle.computeIfAbsent(identity, id -> {
            String handle = "ref-" + counter.incrementAndGet();
            handleToNode.put(handle, new WeakReference<>(node));
            return handle;
        });
    }

    public Optional<Node> resolve(String handle) {
        WeakReference<Node> ref = handleToNode.get(handle);
        if (ref == null) return Optional.empty();
        Node node = ref.get();
        if (node == null) {
            handleToNode.remove(handle);
            return Optional.empty();
        }
        return Optional.of(node);
    }

    public void purgeStale() {
        handleToNode.entrySet().removeIf(e -> e.getValue().get() == null);
        identityToHandle.entrySet().removeIf(e -> !handleToNode.containsKey(e.getValue()));
    }
}

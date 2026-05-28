package com.sicpa.fxagent.scene.selector;

import java.util.List;

public record ChainedSelector(List<Selector> chain) {
    public boolean isSingleSelector() { return chain.size() == 1; }
    public Selector first() { return chain.getFirst(); }
}

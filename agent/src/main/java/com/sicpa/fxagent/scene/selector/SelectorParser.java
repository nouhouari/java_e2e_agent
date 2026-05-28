package com.sicpa.fxagent.scene.selector;

import java.util.Arrays;
import java.util.List;

public final class SelectorParser {

    private SelectorParser() {}

    public static ChainedSelector parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Selector string must not be blank");
        }

        String[] segments = input.split("\\s*>>\\s*");
        List<Selector> chain = Arrays.stream(segments)
                .map(SelectorParser::parseSingle)
                .toList();
        return new ChainedSelector(chain);
    }

    private static Selector parseSingle(String segment) {
        String s = segment.strip();

        if (s.startsWith("#")) {
            return new Selector.Id(s.substring(1));
        }
        if (s.startsWith(".")) {
            return new Selector.StyleClass(s.substring(1));
        }
        if (s.startsWith("text=")) {
            return new Selector.Text(s.substring("text=".length()), true);
        }
        if (s.startsWith("text~=")) {
            return new Selector.Text(s.substring("text~=".length()), false);
        }
        if (s.startsWith("css=")) {
            return new Selector.Css(s.substring("css=".length()));
        }
        if (s.startsWith("ref=")) {
            return new Selector.Handle(s.substring("ref=".length()));
        }
        if (!s.isEmpty() && Character.isUpperCase(s.charAt(0)) && !s.contains(" ")) {
            return new Selector.TypeName(s);
        }

        return new Selector.Css(s);
    }
}

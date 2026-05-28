package com.sicpa.fxagent.api.dto;

import java.util.Map;

public record ActionRequest(
        String handle,
        String selector,
        String action,
        String value,
        Map<String, String> options
) {
    public String option(String key) {
        return options != null ? options.get(key) : null;
    }
}

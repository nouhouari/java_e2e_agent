package com.sicpa.fxagent.api.dto;

public record WaitRequest(
        String selector,
        long timeoutMs,
        long pollIntervalMs,
        String condition
) {}

package com.sicpa.fxagent.api.dto;

public record ScreenshotRequest(
        Integer windowIndex,
        String selector,
        String handle
) {}

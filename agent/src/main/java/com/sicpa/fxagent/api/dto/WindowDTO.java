package com.sicpa.fxagent.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WindowDTO(
        int index,
        String title,
        String type,
        double x,
        double y,
        double width,
        double height,
        boolean focused,
        boolean showing
) {}

package com.sicpa.fxagent.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ElementDTO(
        String handle,
        String id,
        String type,
        String fullType,
        List<String> styleClasses,
        String text,
        BoundsDTO bounds,
        boolean visible,
        boolean enabled,
        boolean focused,
        Map<String, Object> properties,
        List<ElementDTO> children
) {}

package com.sicpa.fxagent.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ActionResponse(boolean success, String message, ElementDTO element) {

    public static ActionResponse success(String message) {
        return new ActionResponse(true, message, null);
    }

    public static ActionResponse success(String message, ElementDTO element) {
        return new ActionResponse(true, message, element);
    }

    public static ActionResponse error(String message) {
        return new ActionResponse(false, message, null);
    }
}

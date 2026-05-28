package com.sicpa.fxagent.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record QueryRequest(
        String selector,
        Integer windowIndex,
        @JsonProperty(defaultValue = "50") int maxResults
) {
    public QueryRequest {
        if (maxResults <= 0) {
            maxResults = 50;
        }
    }
}

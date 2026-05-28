package com.sicpa.fxagent.api.dto;

import java.util.List;

public record QueryResponse(List<ElementDTO> elements, int count) {}

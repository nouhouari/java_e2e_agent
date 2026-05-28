package com.sicpa.fxagent.api.dto;

public record BoundsDTO(double x, double y, double width, double height) {

    public double centerX() {
        return x + width / 2.0;
    }

    public double centerY() {
        return y + height / 2.0;
    }
}

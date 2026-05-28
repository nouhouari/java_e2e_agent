package com.sicpa.fxagent.fx;

import javafx.scene.robot.Robot;

public final class RobotProvider {

    private static volatile Robot instance;

    private RobotProvider() {}

    public static Robot get() {
        if (instance == null) {
            instance = FxExecutor.supplyOnFxThreadBlocking(Robot::new, 5000);
        }
        return instance;
    }
}

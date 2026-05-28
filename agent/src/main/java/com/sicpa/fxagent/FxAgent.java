package com.sicpa.fxagent;

import java.lang.instrument.Instrumentation;

public final class FxAgent {

    public enum AttachMode { PREMAIN, AGENTMAIN }

    private FxAgent() {}

    public static void premain(String agentArgs, Instrumentation inst) {
        AgentBootstrap.start(agentArgs, AttachMode.PREMAIN);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        AgentBootstrap.start(agentArgs, AttachMode.AGENTMAIN);
    }
}

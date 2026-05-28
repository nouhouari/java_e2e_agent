package com.sicpa.fxagent.util;

public final class AgentVersion {

    private static final String VERSION = resolve();

    private AgentVersion() {}

    public static String get() {
        return VERSION;
    }

    private static String resolve() {
        // Implementation-Version is set by the shadowJar manifest at build time
        String fromPackage = AgentVersion.class.getPackage().getImplementationVersion();
        if (fromPackage != null && !fromPackage.isBlank()) {
            return fromPackage;
        }
        return "dev";
    }
}

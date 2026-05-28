package com.sicpa.fxagent.util;

import java.util.HashMap;
import java.util.Map;

public record AgentConfig(int port, long toolkitTimeout, long actionTimeout) {

    private static final int DEFAULT_PORT = 4567;
    private static final long DEFAULT_TOOLKIT_TIMEOUT = 30_000;
    private static final long DEFAULT_ACTION_TIMEOUT = 5_000;

    public static AgentConfig parse(String agentArgs) {
        Map<String, String> args = parseArgs(agentArgs);

        int port = resolveInt(args, "port", "fxagent.port", DEFAULT_PORT);
        long toolkitTimeout = resolveLong(args, "toolkitTimeout", "fxagent.toolkit.timeout", DEFAULT_TOOLKIT_TIMEOUT);
        long actionTimeout = resolveLong(args, "actionTimeout", "fxagent.action.timeout", DEFAULT_ACTION_TIMEOUT);

        return new AgentConfig(port, toolkitTimeout, actionTimeout);
    }

    private static Map<String, String> parseArgs(String agentArgs) {
        Map<String, String> result = new HashMap<>();
        if (agentArgs == null || agentArgs.isBlank()) {
            return result;
        }
        for (String pair : agentArgs.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                result.put(kv[0].trim(), kv[1].trim());
            }
        }
        return result;
    }

    private static int resolveInt(Map<String, String> args, String argKey, String sysProp, int defaultValue) {
        String value = args.get(argKey);
        if (value == null) {
            value = System.getProperty(sysProp);
        }
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private static long resolveLong(Map<String, String> args, String argKey, String sysProp, long defaultValue) {
        String value = args.get(argKey);
        if (value == null) {
            value = System.getProperty(sysProp);
        }
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }
}

package com.sicpa.fxagent.util;

import java.lang.reflect.Method;
import java.util.Optional;

public final class ReflectionUtils {

    private ReflectionUtils() {}

    public static Optional<Object> invokeGetter(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return Optional.ofNullable(method.invoke(target));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static boolean hasMethod(Object target, String methodName) {
        try {
            target.getClass().getMethod(methodName);
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}

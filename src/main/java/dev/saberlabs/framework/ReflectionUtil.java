package dev.saberlabs.framework;

import java.lang.reflect.Method;

/**
 * Pattern 11: REFLECTION FRAMEWORK (Reflection utility)
 *
 * Utility methods for invoking a business object's handler methods by name.
 * Any failure (missing method, reflective access failure, or an exception
 * thrown by the invoked method itself) is logged and swallowed rather than
 * propagated, so a misbehaving handler can't crash the dispatcher.
 */
public class ReflectionUtil {

    public static void invokeMethod(Object obj, String methodName, String parameter) {
        try {
            Method method = obj.getClass().getMethod(methodName, String.class);
            method.invoke(obj, parameter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

package dev.saberlabs.framework.business.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Pattern 11: REFLECTION FRAMEWORK — BUSINESS (Reflection utility)
 *
 * Invokes a handler method reflectively and returns whatever it returns. Unlike
 * {@code dev.saberlabs.framework.example.reflection.ReflectionUtil}, a failure here is NOT
 * swallowed: this dispatches into real business logic (placing a real order, sending a real
 * chat message), so a failure must surface to the caller the same way a direct method call's
 * exception would, rather than being silently logged while the caller carries on as if nothing
 * happened.
 */
public class ReflectionUtil {

    @SuppressWarnings("unchecked")
    public static <R> R invokeMethod(Object obj, Method method, Object parameter) {
        try {
            return (R) method.invoke(obj, parameter);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Handler " + method.getName() + " failed", cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke handler: " + method.getName(), e);
        }
    }
}

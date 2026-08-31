package dev.saberlabs.framework.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Pattern 11: REFLECTION FRAMEWORK (Reflection utility)
 *
 * Utility method for invoking a business object's handler methods by name. Failures propagate
 * to the caller rather than being logged and swallowed: this dispatcher reaches real business
 * logic (real order placement, real chat messages), so a misbehaving handler failing silently
 * would be a correctness/observability regression, not a safety net.
 */
public class ReflectionUtil {

    public static void invokeMethod(Object obj, String methodName, String parameter) {
        try {
            Method method = obj.getClass().getMethod(methodName, String.class);
            // The declaring class (e.g. a package-private real BusinessObject like
            // dev.saberlabs.chat.CoffeeShopBusiness) may not itself be accessible from this
            // package even though the method is public -- Method.invoke checks both.
            method.setAccessible(true);
            method.invoke(obj, parameter);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Handler method threw a checked exception", cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to invoke handler method '" + methodName + "'", e);
        }
    }
}

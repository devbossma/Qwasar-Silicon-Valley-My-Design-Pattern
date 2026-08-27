package dev.saberlabs.framework.business.reflection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Reflection Framework Business: ReflectionUtil")
class ReflectionUtilTest {

    @Test
    @DisplayName("invokeMethod invokes the method and returns its result")
    void invokesMethodAndReturnsResult() throws NoSuchMethodException {
        Target target = new Target();
        Method method = Target.class.getMethod("succeed", String.class);

        String result = ReflectionUtil.invokeMethod(target, method, "hello");

        assertEquals("hello", result);
    }

    @Test
    @DisplayName("invokeMethod propagates a RuntimeException thrown by the invoked method, unlike the example framework's swallow-and-log")
    void propagatesRuntimeExceptionFromInvokedMethod() throws NoSuchMethodException {
        Target target = new Target();
        Method method = Target.class.getMethod("explode", String.class);

        assertThrows(IllegalStateException.class,
                () -> ReflectionUtil.invokeMethod(target, method, "boom"));
    }

    @Test
    @DisplayName("invokeMethod surfaces an argument-type mismatch as a RuntimeException")
    void surfacesArgumentMismatchAsRuntimeException() throws NoSuchMethodException {
        Target target = new Target();
        Method expectsString = Target.class.getMethod("succeed", String.class);

        assertThrows(RuntimeException.class,
                () -> ReflectionUtil.<String>invokeMethod(target, expectsString, 42));
    }

    @Test
    @DisplayName("invokeMethod wraps a checked exception thrown by the invoked method in a RuntimeException")
    void wrapsCheckedExceptionCauseInRuntimeException() throws NoSuchMethodException {
        Target target = new Target();
        Method explodesChecked = Target.class.getMethod("explodeChecked", String.class);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> ReflectionUtil.invokeMethod(target, explodesChecked, "boom"));
        assertInstanceOf(java.io.IOException.class, thrown.getCause());
    }

    static class Target {
        public String succeed(String value) {
            return value;
        }

        public void explode(String value) {
            throw new IllegalStateException("boom");
        }

        public void explodeChecked(String value) throws java.io.IOException {
            throw new java.io.IOException("checked boom");
        }
    }
}

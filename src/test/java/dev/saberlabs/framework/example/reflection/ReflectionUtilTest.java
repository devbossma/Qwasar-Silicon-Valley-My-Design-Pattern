package dev.saberlabs.framework.example.reflection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Reflection Framework Example: ReflectionUtil")
class ReflectionUtilTest {

    @Test
    @DisplayName("invokeMethod calls the named method with the given String argument")
    void invokesMethodWithArgument() {
        Target target = new Target();

        ReflectionUtil.invokeMethod(target, "succeed", "hello");

        assertEquals("hello", target.received);
    }

    @Test
    @DisplayName("invokeMethod swallows an exception thrown by the invoked method")
    void swallowsExceptionFromInvokedMethod() {
        Target target = new Target();

        assertDoesNotThrow(() -> ReflectionUtil.invokeMethod(target, "explode", "boom"));

        assertNull(target.received);
    }

    @Test
    @DisplayName("invokeMethod swallows a lookup failure for a nonexistent method name")
    void swallowsNoSuchMethod() {
        Target target = new Target();

        assertDoesNotThrow(() -> ReflectionUtil.invokeMethod(target, "doesNotExist", "x"));

        assertNull(target.received);
    }

    static class Target {
        String received;

        public void succeed(String value) {
            received = value;
        }

        public void explode(String value) {
            throw new RuntimeException("boom");
        }
    }
}

package dev.saberlabs.framework.reflection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Reflection Framework: ReflectionUtil")
class ReflectionUtilTest {

    @Test
    @DisplayName("invokeMethod calls the named method with the given String argument")
    void invokesMethodWithArgument() {
        Target target = new Target();

        ReflectionUtil.invokeMethod(target, "succeed", "hello");

        assertEquals("hello", target.received);
    }

    @Test
    @DisplayName("invokeMethod propagates a RuntimeException thrown by the invoked method, "
            + "since this dispatcher reaches real business logic and shouldn't fail silently")
    void propagatesRuntimeExceptionFromInvokedMethod() {
        Target target = new Target();

        assertThrows(IllegalStateException.class,
                () -> ReflectionUtil.invokeMethod(target, "explode", "boom"));
    }

    @Test
    @DisplayName("invokeMethod wraps a checked exception thrown by the invoked method in a RuntimeException")
    void wrapsCheckedExceptionCauseInRuntimeException() {
        Target target = new Target();

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> ReflectionUtil.invokeMethod(target, "explodeChecked", "boom"));
        assertInstanceOf(IOException.class, thrown.getCause());
    }

    @Test
    @DisplayName("invokeMethod surfaces a lookup failure for a nonexistent method name as a RuntimeException")
    void surfacesNoSuchMethodAsRuntimeException() {
        Target target = new Target();

        assertThrows(RuntimeException.class,
                () -> ReflectionUtil.invokeMethod(target, "doesNotExist", "x"));
    }

    static class Target {
        String received;

        public void succeed(String value) {
            received = value;
        }

        public void explode(String value) {
            throw new IllegalStateException("boom");
        }

        public void explodeChecked(String value) throws IOException {
            throw new IOException("checked boom");
        }
    }
}

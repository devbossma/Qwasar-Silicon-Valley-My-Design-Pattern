package dev.saberlabs.fx;

import dev.saberlabs.db.DatabaseUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

/**
 * Shared base for TestFX controller tests.
 *
 * CRITICAL ORDERING REQUIREMENT: setDbPathForTesting() MUST be called
 * in a static initializer or @BeforeAll — BEFORE JUnit/TestFX's
 * ApplicationExtension triggers any @Start method, and before any
 * other test class in the same JVM run touches AppContext.getInstance().
 *
 * The safest guarantee: call this from a static block in EACH test
 * class that extends this base, since static blocks run at class-load
 * time, which happens before JUnit invokes any lifecycle method
 * including @Start.
 */
public abstract class FxTestBase {

    protected static void configureTestDatabase() {
        // Idempotent — safe to call from multiple test classes; the
        // FIRST call wins, since DatabaseUtil's connection is a singleton
        // that's created lazily on first getConnection().
        String tempPath = System.getProperty("java.io.tmpdir")
                + "/coffee-chat-test-" + System.nanoTime() + ".db";
        DatabaseUtil.setDbPathForTesting(tempPath);
    }
}
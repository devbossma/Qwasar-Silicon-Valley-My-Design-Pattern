package dev.saberlabs.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises resolveDbPath()/prodDataDirectory() directly rather than through
 * getConnection() -- both are pure, side-effect-free functions of system
 * properties/env vars, so testing them this way never touches the real
 * filesystem or creates a real OS app-data directory on the test machine.
 *
 * Separate from DatabaseUtilTest because every one of its tests relies on
 * setDbPathForTesting() being active; this class specifically needs that
 * override cleared to reach the dev/prod branches underneath it.
 */
@DisplayName("DatabaseUtil path resolution")
class DatabaseUtilPathResolutionTest {

    private static final String ENV_PROPERTY = "coffeeshop.env";
    private static final String OS_PROPERTY  = "os.name";

    private String originalEnvProperty;
    private String originalOsName;

    @BeforeEach
    void setUp() {
        // Save the original system properties so we can restore them after each test
        originalEnvProperty = System.getProperty(ENV_PROPERTY);
        // Save the original OS name so we can restore it after each test
        originalOsName = System.getProperty(OS_PROPERTY);
        DatabaseUtil.clearDbPathOverrideForTesting();
    }

    @AfterEach
    void tearDown() {
        // Restore the original system properties after each test
        restoreProperty(ENV_PROPERTY, originalEnvProperty);
        // Restore the original OS name after each test
        restoreProperty(OS_PROPERTY, originalOsName);
        DatabaseUtil.clearDbPathOverrideForTesting();
    }

    /**
     * Restores a system property to its original value, or clears it if the
     * original value was null.
     *
     * @param key           the system property key
     * @param originalValue the original value of the system property, or null if it was unset
     */
    private void restoreProperty(String key, String originalValue) {
        if (originalValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, originalValue);
        }
    }

    @Nested
    @DisplayName("resolveDbPath()")
    class ResolveDbPathTests {

        @Test
        @DisplayName("uses the test override when one is set, ignoring coffeeshop.env entirely")
        void prefersTestOverride() {
            System.setProperty(ENV_PROPERTY, "dev");
            DatabaseUtil.setDbPathForTesting("some/override/path.db");

            assertEquals("some/override/path.db", DatabaseUtil.resolveDbPath());
        }

        @Test
        @DisplayName("resolves to the project-relative data/ path when coffeeshop.env=dev")
        void devEnvResolvesToRelativePath() {
            System.setProperty(ENV_PROPERTY, "dev");

            assertEquals("data/coffee-chat.db", DatabaseUtil.resolveDbPath());
        }

        @Test
        @DisplayName("coffeeshop.env is case-insensitive")
        void devEnvIsCaseInsensitive() {
            System.setProperty(ENV_PROPERTY, "DEV");

            assertEquals("data/coffee-chat.db", DatabaseUtil.resolveDbPath());
        }

        @Test
        @DisplayName("resolves under the OS app-data directory when coffeeshop.env=prod")
        void prodEnvResolvesUnderAppDataDirectory() {
            System.setProperty(ENV_PROPERTY, "prod");

            String resolved = DatabaseUtil.resolveDbPath();

            assertEquals(DatabaseUtil.prodDataDirectory().resolve("coffee-chat.db").toString(),
                    resolved);
        }

        @Test
        @DisplayName("defaults to prod when coffeeshop.env is unset")
        void defaultsToProdWhenUnset() {
            System.clearProperty(ENV_PROPERTY);

            String resolved = DatabaseUtil.resolveDbPath();

            assertEquals(DatabaseUtil.prodDataDirectory().resolve("coffee-chat.db").toString(),
                    resolved);
        }
    }

    @Nested
    @DisplayName("prodDataDirectory()")
    class ProdDataDirectoryTests {

        @Test
        @DisplayName("on Windows, resolves under LOCALAPPDATA (or user.home\\AppData\\Local) \\ CoffeeShopApp")
        void windowsUsesLocalAppData() {
            System.setProperty(OS_PROPERTY, "Windows 11");

            Path resolved = DatabaseUtil.prodDataDirectory();

            String localAppData = System.getenv("LOCALAPPDATA");
            Path expectedBase = localAppData != null
                    ? Path.of(localAppData)
                    : Path.of(System.getProperty("user.home"), "AppData", "Local");
            assertEquals(expectedBase.resolve("CoffeeShopApp"), resolved);
        }

        @Test
        @DisplayName("on macOS, resolves under ~/Library/Application Support/CoffeeShopApp")
        void macUsesApplicationSupport() {
            System.setProperty(OS_PROPERTY, "Mac OS X");

            Path resolved = DatabaseUtil.prodDataDirectory();

            Path expected = Path.of(System.getProperty("user.home"),
                    "Library", "Application Support", "CoffeeShopApp");
            assertEquals(expected, resolved);
        }

        @Test
        @DisplayName("on Linux, resolves under XDG_DATA_HOME (or ~/.local/share) / CoffeeShopApp")
        void linuxUsesXdgDataHomeOrLocalShare() {
            System.setProperty(OS_PROPERTY, "Linux");

            Path resolved = DatabaseUtil.prodDataDirectory();

            String xdgDataHome = System.getenv("XDG_DATA_HOME");
            Path expectedBase = xdgDataHome != null
                    ? Path.of(xdgDataHome)
                    : Path.of(System.getProperty("user.home"), ".local", "share");
            assertEquals(expectedBase.resolve("CoffeeShopApp"), resolved);
        }

        @Test
        @DisplayName("always resolves to a path ending in the app directory name")
        void alwaysEndsInAppDirectoryName() {
            Path resolved = DatabaseUtil.prodDataDirectory();

            assertEquals("CoffeeShopApp", resolved.getFileName().toString());
        }
    }
}

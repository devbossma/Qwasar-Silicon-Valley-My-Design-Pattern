package dev.saberlabs.db;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the SQLite database connection for the coffee shop chat application.
 *
 * Schema is defined in src/main/resources/schema.sql rather than as Java
 * string literals — this keeps the schema readable, lets IDE SQL tooling
 * validate it directly, and matches the standard Flyway/Liquibase convention
 * of versioning schema as plain .sql files.
 * *
 * One JDBC {@link Connection} per thread, not one shared globally: every
 * repository method does {@code DatabaseUtil.getConnection()} then a single
 * statement, so each thread (worker Baristas, FX event threads, request
 * handlers) gets its own connection lazily on first use, and nobody blocks
 * on -- or corrupts the cursor state of -- a connection another thread is
 * mid-query on. SQLite serializes writes at the file level regardless, but
 * concurrent reads and prepared-statement state are now thread-isolated too,
 * and the design carries over cleanly if the backend ever stops being
 * single-file SQLite.
 */
public class DatabaseUtil {

    // Switches where the SQLite file lives: "dev" keeps the old CWD-relative
    // path (convenient when running from the IDE/mvn), anything else (or
    // unset) resolves to an OS-appropriate per-user app-data directory -- the
    // default for the distributed jar, since a relative path resolves against
    // whatever directory `java -jar` happens to be launched from, not the
    // jar's own location.
    private static final String ENV_PROPERTY  = "coffeeshop.env";
    private static final String DEV_DB_FILE   = "data/coffee-chat.db";
    private static final String APP_DIR_NAME  = "CoffeeShopApp";
    private static final String DB_FILE_NAME  = "coffee-chat.db";
    private static final String URL     = "jdbc:sqlite:";
    private static final String SCHEMA_RESOURCE = "/schema.sql";

    // Now that each thread opens its own connection to the same SQLite file,
    // a writer on one thread can make another thread's connection hit
    // SQLITE_BUSY. This makes the second connection wait for the lock
    // instead of failing immediately.
    private static final int BUSY_TIMEOUT_MS = 5000;

    // For testing purposes, allows overriding the database path to a temporary file.
    private static volatile String dbPathOverride = null;

    private static final ThreadLocal<Connection> connectionHolder = new ThreadLocal<>();

    // Tracked so closeAllConnections() can close connections opened by threads
    // other than the caller (e.g. worker Baristas) during test/app shutdown.
    private static final Set<Connection> openConnections =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    private DatabaseUtil() { }

    public static @NotNull Connection getConnection() {
        Connection connection = connectionHolder.get();
        if (connection == null) {
            try {
                String path = resolveDbPath();
                Files.createDirectories(Path.of(path).getParent());
                connection = DriverManager.getConnection(URL + path);
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MS);
                }
                connectionHolder.set(connection);
                openConnections.add(connection);
                System.out.println("[DatabaseUtil] Connected to: " + path
                        + " (thread: " + Thread.currentThread().getName() + ")");
            } catch (SQLException e) {
                throw new RuntimeException("Failed to connect to the database", e);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create data directory", e);
            }
        }
        return connection;
    }

    /**
     * Closes the calling thread's own connection, if it has one.
     */
    public static void closeConnection() {
        Connection connection = connectionHolder.get();
        if (connection != null) {
            closeQuietly(connection);
            openConnections.remove(connection);
            connectionHolder.remove();
        }
    }

    /**
     * Closes every connection opened by any thread. Intended for application
     * shutdown and test teardown, where per-thread {@link #closeConnection()}
     * can't reach connections opened by threads that have already exited.
     */
    public static void closeAllConnections() {
        for (Connection connection : openConnections) {
            closeQuietly(connection);
        }
        openConnections.clear();
        connectionHolder.remove();
    }

    private static void closeQuietly(@NotNull Connection connection) {
        try {
            connection.close();
            System.out.println("[DatabaseUtil] Connection closed.");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to close database connection", e);
        }
    }

    /**
     * Overrides the database file path — must be called BEFORE the first
     * call to getConnection() or initialize(). Intended for test isolation;
     * production code should never call this.
     */
    public static void setDbPathForTesting(@NotNull String path) {
        dbPathOverride = path;
    }

    /**
     * Clears the test override so resolveDbPath() falls through to the
     * dev/prod system-property logic. Package-private: only
     * DatabaseUtilPathResolutionTest (same package) needs this to test that
     * logic in isolation from the override every other test relies on.
     */
    static void clearDbPathOverrideForTesting() {
        dbPathOverride = null;
    }

    /**
     * Resolves where the SQLite file should live, in priority order:
     * an explicit test override, then the {@value #ENV_PROPERTY} system
     * property. Read fresh on every call (not cached) so the first
     * getConnection() in a process always reflects the current setting.
     * Package-private (not private) so DatabaseUtilPathResolutionTest can
     * exercise it directly without going through a real getConnection().
     */
    static @NotNull String resolveDbPath() {
        if (dbPathOverride != null) {
            return dbPathOverride;
        }
        String env = System.getProperty(ENV_PROPERTY, "prod");
        if ("dev".equalsIgnoreCase(env)) {
            return DEV_DB_FILE;
        }
        return prodDataDirectory().resolve(DB_FILE_NAME).toString();
    }

    /**
     * The OS-appropriate per-user application-data directory, matching each
     * platform's own convention rather than writing next to the jar (which
     * may not even be writable, e.g. under Program Files) or the directory
     * the jar happened to be launched from.
     */
    static @NotNull Path prodDataDirectory() {
        String userHome = System.getProperty("user.home");
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);

        if (os.contains("win")) {
            String localAppData = System.getenv("LOCALAPPDATA");
            Path base = localAppData != null
                    ? Path.of(localAppData) : Path.of(userHome, "AppData", "Local");
            return base.resolve(APP_DIR_NAME);
        }
        if (os.contains("mac")) {
            return Path.of(userHome, "Library", "Application Support", APP_DIR_NAME);
        }
        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        Path base = xdgDataHome != null
                ? Path.of(xdgDataHome) : Path.of(userHome, ".local", "share");
        return base.resolve(APP_DIR_NAME);
    }

    public static void execSQL(@NotNull String sql) {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL: " + sql, e);
        }
    }

    /**
     * Initializes the database by reading and executing schema.sql
     * from the classpath. Splits on semicolons so each CREATE TABLE
     * statement runs as its own Statement.execute() call.
     */
    public static void initialize() {
        String schema = readSchemaResource();
        Arrays.stream(schema.split(";"))
                .map(String::trim)
                .filter(stmt -> !stmt.isEmpty())
                .forEach(DatabaseUtil::execSQL);

        System.out.println("[DatabaseUtil] Database initialized.");
    }

    private static @NotNull String readSchemaResource() {
        try (InputStream in = DatabaseUtil.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new RuntimeException(
                        "schema.sql not found on classpath at " + SCHEMA_RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    // Skip pure comment lines, keep everything else
                    if (!line.trim().startsWith("--")) {
                        sb.append(line).append('\n');
                    }
                }
                return sb.toString();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read schema.sql", e);
        }
    }
}
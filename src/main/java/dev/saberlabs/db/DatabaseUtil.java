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

/**
 * Manages the SQLite database connection for the coffee shop chat application.
 *
 * Schema is defined in src/main/resources/schema.sql rather than as Java
 * string literals — this keeps the schema readable, lets IDE SQL tooling
 * validate it directly, and matches the standard Flyway/Liquibase convention
 * of versioning schema as plain .sql files.
 */
public class DatabaseUtil {

    private static final String DB_DIR  = "data";
    private static final String DB_FILE = "data/coffee-chat.db";
    private static final String URL     = "jdbc:sqlite:";
    private static final String SCHEMA_RESOURCE = "/schema.sql";

    // For testing purposes, allows overriding the database path to a temporary file.
    private static String dbPathOverride = null;

    private static volatile Connection connection = null;

    private DatabaseUtil() { }

    public static @NotNull Connection getConnection() {
        if (connection == null) {
            synchronized (DatabaseUtil.class) {
                if (connection == null) {
                    try {
                        String path = dbPathOverride != null ? dbPathOverride : DB_FILE;
                        Files.createDirectories(Path.of(path).getParent());
                        connection = DriverManager.getConnection(URL + path);
                        System.out.println("[DatabaseUtil] Connected to: " + path);
                    } catch (SQLException e) {
                        throw new RuntimeException("Failed to connect to the database", e);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to create data directory", e);
                    }
                }
            }
        }
        return connection;
    }

    public static void closeConnection() {
        synchronized (DatabaseUtil.class) {
            if (connection != null) {
                try {
                    connection.close();
                    System.out.println("[DatabaseUtil] Connection closed.");
                } catch (SQLException e) {
                    throw new RuntimeException(
                            "Failed to close database connection", e);
                } finally {
                    connection = null;
                }
            }
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
package dev.saberlabs.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseUtil")
class DatabaseUtilTest {

    private static final int EXPECTED_BUSY_TIMEOUT_MS = 5000;
    private static final List<String> EXPECTED_TABLES = List.of(
            "users", "chat_sessions", "messages", "orders", "notifications", "image_uploads");

    private Path tempDbFile;

    @BeforeEach
    void setUp() throws IOException {
        // Start every test with a clean slate: no leftover connection from
        // whatever ran before us in this JVM, and a fresh temp DB file.
        DatabaseUtil.closeAllConnections();
        tempDbFile = Files.createTempFile("coffee-chat-db-util-test-", ".db");
        Files.deleteIfExists(tempDbFile);
        DatabaseUtil.setDbPathForTesting(tempDbFile.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        DatabaseUtil.closeAllConnections();
        Files.deleteIfExists(tempDbFile);
    }

    @Nested
    @DisplayName("getConnection()")
    class GetConnectionTests {

        @Test
        @DisplayName("returns the same Connection instance on repeated calls from the same thread")
        void reusesConnectionOnSameThread() {
            Connection first = DatabaseUtil.getConnection();
            Connection second = DatabaseUtil.getConnection();

            assertSame(first, second, "Same thread should reuse its cached connection");
        }

        @Test
        @DisplayName("returns a different Connection instance for a different thread")
        void separateConnectionPerThread() throws InterruptedException {
            Connection mainThreadConnection = DatabaseUtil.getConnection();

            AtomicReference<Connection> otherThreadConnection = new AtomicReference<>();
            Thread worker = new Thread(() -> otherThreadConnection.set(DatabaseUtil.getConnection()));
            worker.start();
            worker.join();

            assertNotSame(mainThreadConnection, otherThreadConnection.get(),
                    "Each thread should get its own Connection, not a shared one");

            DatabaseUtil.closeConnection(); // clean up the worker thread's own connection
        }

        @Test
        @DisplayName("new connections have PRAGMA busy_timeout set")
        void setsBusyTimeoutPragma() throws SQLException {
            Connection connection = DatabaseUtil.getConnection();
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA busy_timeout")) {
                assertTrue(rs.next());
                assertEquals(EXPECTED_BUSY_TIMEOUT_MS, rs.getInt(1));
            }
        }
    }

    @Nested
    @DisplayName("closeConnection()")
    class CloseConnectionTests {

        @Test
        @DisplayName("closes only the calling thread's own connection")
        void closesOnlyCallingThreadsConnection() throws InterruptedException, SQLException {
            Connection mainThreadConnection = DatabaseUtil.getConnection();

            AtomicReference<Connection> otherThreadConnection = new AtomicReference<>();
            CountDownLatch otherThreadConnected = new CountDownLatch(1);
            Thread worker = new Thread(() -> {
                otherThreadConnection.set(DatabaseUtil.getConnection());
                otherThreadConnected.countDown();
            });
            worker.start();
            assertTrue(otherThreadConnected.await(5, TimeUnit.SECONDS));
            worker.join();

            DatabaseUtil.closeConnection();

            assertTrue(mainThreadConnection.isClosed(), "Calling thread's connection should be closed");
            assertFalse(otherThreadConnection.get().isClosed(),
                    "Other thread's connection should be untouched");
        }
    }

    @Nested
    @DisplayName("closeAllConnections()")
    class CloseAllConnectionsTests {

        @Test
        @DisplayName("closes connections opened by every thread, not just the caller")
        void closesEveryThreadsConnection() throws InterruptedException, SQLException {
            Connection mainThreadConnection = DatabaseUtil.getConnection();

            AtomicReference<Connection> workerOneConnection = new AtomicReference<>();
            AtomicReference<Connection> workerTwoConnection = new AtomicReference<>();
            Thread workerOne = new Thread(() -> workerOneConnection.set(DatabaseUtil.getConnection()));
            Thread workerTwo = new Thread(() -> workerTwoConnection.set(DatabaseUtil.getConnection()));
            workerOne.start();
            workerTwo.start();
            workerOne.join();
            workerTwo.join();

            DatabaseUtil.closeAllConnections();

            assertTrue(mainThreadConnection.isClosed());
            assertTrue(workerOneConnection.get().isClosed());
            assertTrue(workerTwoConnection.get().isClosed());
        }
    }

    @Nested
    @DisplayName("initialize() / execSQL()")
    class SchemaInitializationTests {

        @Test
        @DisplayName("creates every table defined in schema.sql")
        void createsAllExpectedTables() throws SQLException {
            DatabaseUtil.initialize();

            Set<String> actualTables = new HashSet<>();
            try (Statement stmt = DatabaseUtil.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT name FROM sqlite_master WHERE type = 'table'")) {
                while (rs.next()) {
                    actualTables.add(rs.getString("name"));
                }
            }

            for (String expectedTable : EXPECTED_TABLES) {
                assertTrue(actualTables.contains(expectedTable),
                        "Expected table missing after initialize(): " + expectedTable);
            }
        }

        @Test
        @DisplayName("execSQL() executes an arbitrary statement against the connection")
        void execSqlRunsArbitraryStatement() throws SQLException {
            DatabaseUtil.execSQL("CREATE TABLE probe (id INTEGER PRIMARY KEY)");

            try (Statement stmt = DatabaseUtil.getConnection().createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'probe'")) {
                assertTrue(rs.next(), "Table created by execSQL() should exist");
            }
        }
    }
}

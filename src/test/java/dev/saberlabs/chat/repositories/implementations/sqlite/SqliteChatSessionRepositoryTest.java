package dev.saberlabs.chat.repositories.implementations.sqlite;

import dev.saberlabs.chat.ChatSession;
import dev.saberlabs.chat.SessionStatus;
import dev.saberlabs.db.DatabaseUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises SqliteChatSessionRepository against a real temp-file SQLite database,
 * following the pattern established by SqliteUserRepositoryTest.
 */
@DisplayName("SqliteChatSessionRepository")
class SqliteChatSessionRepositoryTest {

    private Path tempDbFile;
    private SqliteChatSessionRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        DatabaseUtil.closeAllConnections();
        tempDbFile = Files.createTempFile("coffee-chat-session-repo-test-", ".db");
        Files.deleteIfExists(tempDbFile);
        DatabaseUtil.setDbPathForTesting(tempDbFile.toString());
        DatabaseUtil.initialize();
        repository = new SqliteChatSessionRepository();
    }

    @AfterEach
    void tearDown() throws IOException {
        DatabaseUtil.closeAllConnections();
        Files.deleteIfExists(tempDbFile);
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("inserts a new session (id==0) and assigns a generated ID")
        void insertsNewSession() {
            ChatSession saved = repository.save(ChatSession.newWaitingSession(1L));

            assertTrue(saved.id() > 0);
            assertEquals(SessionStatus.WAITING, saved.status());
            assertNull(saved.baristaId());
        }

        @Test
        @DisplayName("updates an existing session (id!=0) in place")
        void updatesExistingSession() {
            ChatSession saved = repository.save(ChatSession.newWaitingSession(1L));
            ChatSession assigned = saved.assignTo(100L);

            repository.save(assigned);

            Optional<ChatSession> found = repository.findById(saved.id());
            assertTrue(found.isPresent());
            assertEquals(SessionStatus.ACTIVE, found.get().status());
            assertEquals(100L, found.get().baristaId());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("finds a previously saved session, mapping a null barista_id correctly")
        void findsExistingSession() {
            ChatSession saved = repository.save(ChatSession.newWaitingSession(2L));

            Optional<ChatSession> found = repository.findById(saved.id());

            assertTrue(found.isPresent());
            assertEquals(2L, found.get().customerId());
            assertNull(found.get().baristaId());
        }

        @Test
        @DisplayName("returns empty for an unknown ID")
        void returnsEmptyWhenNotFound() {
            assertTrue(repository.findById(999L).isEmpty());
        }
    }

    @Nested
    @DisplayName("findActiveSessionByCustomer()")
    class FindActiveSessionByCustomerTests {

        @Test
        @DisplayName("finds the customer's non-inactive session")
        void findsActiveSession() {
            ChatSession saved = repository.save(ChatSession.newWaitingSession(3L));

            Optional<ChatSession> found = repository.findActiveSessionByCustomer(3L);

            assertTrue(found.isPresent());
            assertEquals(saved.id(), found.get().id());
        }

        @Test
        @DisplayName("returns empty when the customer has no active session")
        void returnsEmptyWhenNoActiveSession() {
            assertTrue(repository.findActiveSessionByCustomer(999L).isEmpty());
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("returns every saved session")
        void returnsAllSessions() {
            repository.save(ChatSession.newWaitingSession(4L));
            repository.save(ChatSession.newWaitingSession(5L));

            assertEquals(2, repository.findAll().size());
        }
    }

    @Nested
    @DisplayName("findActiveSessionsByBarista()")
    class FindActiveSessionsByBaristaTests {

        @Test
        @DisplayName("returns only ACTIVE sessions assigned to the given barista")
        void filtersByBarista() {
            ChatSession waiting = repository.save(ChatSession.newWaitingSession(6L));
            repository.save(waiting.assignTo(200L));
            repository.save(ChatSession.newWaitingSession(7L));

            List<ChatSession> results = repository.findActiveSessionsByBarista(200L);

            assertEquals(1, results.size());
            assertEquals(6L, results.get(0).customerId());
        }

        @Test
        @DisplayName("returns an empty list when the barista has no active sessions")
        void returnsEmptyWhenNoneAssigned() {
            assertTrue(repository.findActiveSessionsByBarista(999L).isEmpty());
        }
    }

    /**
     * Covers the plain catch (SQLException e) branch every read/write method
     * falls back to for a driver-level failure that isn't a constraint
     * violation -- e.g. the underlying table being gone.
     */
    @Nested
    @DisplayName("Generic database failure (not a constraint violation)")
    class DatabaseFailureTests {

        @Test
        @DisplayName("findById() wraps a SQLException in a RuntimeException")
        void findByIdWrapsSqlExceptionInRuntimeException() {
            DatabaseUtil.execSQL("DROP TABLE chat_sessions");

            assertThrows(RuntimeException.class, () -> repository.findById(1L));
        }
    }
}

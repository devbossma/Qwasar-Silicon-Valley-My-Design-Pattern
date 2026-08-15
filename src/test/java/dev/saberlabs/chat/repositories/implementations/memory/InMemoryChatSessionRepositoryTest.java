package dev.saberlabs.chat.repositories.implementations.memory;

import dev.saberlabs.chat.ChatSession;
import dev.saberlabs.chat.SessionStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryChatSessionRepository")
class InMemoryChatSessionRepositoryTest {

    private InMemoryChatSessionRepository repo;

    @BeforeEach
    void setUp() {
        repo = new InMemoryChatSessionRepository();
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("inserts a new session (id==0) and assigns a generated ID")
        void insertsNewSession() {
            ChatSession saved = repo.save(ChatSession.newWaitingSession(1L));

            assertTrue(saved.id() > 0);
            assertEquals(SessionStatus.WAITING, saved.status());
        }

        @Test
        @DisplayName("replaces an existing session (id!=0) in place")
        void updatesExistingSession() {
            ChatSession saved = repo.save(ChatSession.newWaitingSession(1L));
            ChatSession assigned = saved.assignTo(100L);

            repo.save(assigned);

            Optional<ChatSession> found = repo.findById(saved.id());
            assertTrue(found.isPresent());
            assertEquals(SessionStatus.ACTIVE, found.get().status());
            assertEquals(1, repo.findAll().size());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("returns empty for an unknown ID")
        void returnsEmptyWhenNotFound() {
            assertTrue(repo.findById(999L).isEmpty());
        }
    }

    @Nested
    @DisplayName("findActiveSessionByCustomer()")
    class FindActiveSessionByCustomerTests {

        @Test
        @DisplayName("finds the customer's non-inactive session")
        void findsActiveSession() {
            ChatSession saved = repo.save(ChatSession.newWaitingSession(2L));

            Optional<ChatSession> found = repo.findActiveSessionByCustomer(2L);

            assertTrue(found.isPresent());
            assertEquals(saved.id(), found.get().id());
        }

        @Test
        @DisplayName("excludes INACTIVE sessions")
        void excludesInactiveSessions() {
            ChatSession saved = repo.save(ChatSession.newWaitingSession(3L));
            repo.save(saved.deactivate());

            assertTrue(repo.findActiveSessionByCustomer(3L).isEmpty());
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("returns every saved session")
        void returnsAllSessions() {
            repo.save(ChatSession.newWaitingSession(4L));
            repo.save(ChatSession.newWaitingSession(5L));

            assertEquals(2, repo.findAll().size());
        }
    }

    @Nested
    @DisplayName("findActiveSessionsByBarista()")
    class FindActiveSessionsByBaristaTests {

        @Test
        @DisplayName("returns only ACTIVE sessions assigned to the given barista")
        void filtersByBarista() {
            ChatSession waiting = repo.save(ChatSession.newWaitingSession(6L));
            repo.save(waiting.assignTo(200L));
            repo.save(ChatSession.newWaitingSession(7L));

            List<ChatSession> results = repo.findActiveSessionsByBarista(200L);

            assertEquals(1, results.size());
            assertEquals(6L, results.get(0).customerId());
        }
    }

    @Nested
    @DisplayName("clear()")
    class ClearTests {

        @Test
        @DisplayName("removes every stored session")
        void removesAllSessions() {
            repo.save(ChatSession.newWaitingSession(8L));

            repo.clear();

            assertTrue(repo.findAll().isEmpty());
        }
    }
}

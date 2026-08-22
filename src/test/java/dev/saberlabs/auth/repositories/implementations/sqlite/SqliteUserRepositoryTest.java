package dev.saberlabs.auth.repositories.implementations.sqlite;

import dev.saberlabs.auth.Role;
import dev.saberlabs.auth.User;
import dev.saberlabs.db.DatabaseUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Representative Sqlite*Repository test — exercises SqliteUserRepository
 * against a real temp-file SQLite database rather than mocking JDBC, since
 * the whole point is proving the SQL actually works against the real schema.
 */
@DisplayName("SqliteUserRepository")
class SqliteUserRepositoryTest {

    private Path tempDbFile;
    private SqliteUserRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        DatabaseUtil.closeAllConnections();
        tempDbFile = Files.createTempFile("coffee-chat-user-repo-test-", ".db");
        Files.deleteIfExists(tempDbFile);
        DatabaseUtil.setDbPathForTesting(tempDbFile.toString());
        DatabaseUtil.initialize();
        repository = new SqliteUserRepository();
    }

    @AfterEach
    void tearDown() throws IOException {
        DatabaseUtil.closeAllConnections();
        Files.deleteIfExists(tempDbFile);
    }

    private User newUser(String username, Role role) {
        return new User(0, username, "hashed-password", role, LocalDateTime.now());
    }

    @Nested
    @DisplayName("save()")
    class SaveTests {

        @Test
        @DisplayName("assigns a generated ID and preserves the other fields")
        void assignsGeneratedId() {
            User saved = repository.save(newUser("alice", Role.CUSTOMER));

            assertTrue(saved.id() > 0);
            assertEquals("alice", saved.username());
            assertEquals(Role.CUSTOMER, saved.role());
        }
    }

    @Nested
    @DisplayName("findByUsername()")
    class FindByUsernameTests {

        @Test
        @DisplayName("finds a previously saved user")
        void findsExistingUser() {
            repository.save(newUser("barista_bob", Role.BARISTA));

            Optional<User> found = repository.findByUsername("barista_bob");

            assertTrue(found.isPresent());
            assertEquals(Role.BARISTA, found.get().role());
        }

        @Test
        @DisplayName("returns empty for an unknown username")
        void returnsEmptyWhenNotFound() {
            assertTrue(repository.findByUsername("nobody").isEmpty());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindByIdTests {

        @Test
        @DisplayName("finds a previously saved user by its generated ID")
        void findsExistingUser() {
            User saved = repository.save(newUser("carol", Role.CUSTOMER));

            Optional<User> found = repository.findById(saved.id());

            assertTrue(found.isPresent());
            assertEquals("carol", found.get().username());
        }

        @Test
        @DisplayName("returns empty for an unknown ID")
        void returnsEmptyWhenNotFound() {
            assertTrue(repository.findById(999L).isEmpty());
        }
    }

    @Nested
    @DisplayName("findAll()")
    class FindAllTests {

        @Test
        @DisplayName("returns every saved user")
        void returnsAllUsers() {
            repository.save(newUser("dave", Role.CUSTOMER));
            repository.save(newUser("erin", Role.BARISTA));

            List<User> all = repository.findAll();

            assertEquals(2, all.size());
        }

        @Test
        @DisplayName("returns an empty list when no users exist")
        void returnsEmptyWhenNoUsers() {
            assertTrue(repository.findAll().isEmpty());
        }
    }

    @Nested
    @DisplayName("findByRole()")
    class FindByRoleTests {

        @Test
        @DisplayName("returns only users with the matching role")
        void filtersByRole() {
            repository.save(newUser("frank", Role.CUSTOMER));
            repository.save(newUser("grace", Role.BARISTA));
            repository.save(newUser("heidi", Role.BARISTA));

            List<User> baristas = repository.findByRole(Role.BARISTA);

            assertEquals(2, baristas.size());
            assertTrue(baristas.stream().allMatch(u -> u.role() == Role.BARISTA));
        }
    }

    @Nested
    @DisplayName("deleteById()")
    class DeleteByIdTests {

        @Test
        @DisplayName("removes an existing user and returns true")
        void deletesExistingUser() {
            User saved = repository.save(newUser("ivan", Role.CUSTOMER));

            boolean deleted = repository.deleteById(saved.id());

            assertTrue(deleted);
            assertTrue(repository.findById(saved.id()).isEmpty());
        }

        @Test
        @DisplayName("returns false for an unknown ID")
        void returnsFalseWhenNotFound() {
            assertFalse(repository.deleteById(999L));
        }
    }

    @Nested
    @DisplayName("existsByUsername()")
    class ExistsByUsernameTests {

        @Test
        @DisplayName("true for a saved username, false otherwise")
        void checksExistence() {
            repository.save(newUser("judy", Role.CUSTOMER));

            assertTrue(repository.existsByUsername("judy"));
            assertFalse(repository.existsByUsername("nobody"));
        }
    }

    @Nested
    @DisplayName("updatePassword()")
    class UpdatePasswordTests {

        @Test
        @DisplayName("persists the new password hash for an existing user")
        void updatesExistingUser() {
            User saved = repository.save(newUser("mallory", Role.CUSTOMER));
            User withNewPassword = new User(saved.id(), saved.username(),
                    "new-hashed-password", saved.role(), saved.createdAt());

            repository.updatePassword(withNewPassword);

            Optional<User> reloaded = repository.findById(saved.id());
            assertTrue(reloaded.isPresent());
            assertEquals("new-hashed-password", reloaded.get().passwordHash());
        }

        @Test
        @DisplayName("throws for a user ID that doesn't exist")
        void throwsForUnknownUser() {
            User ghost = new User(999L, "ghost", "hash", Role.CUSTOMER, LocalDateTime.now());

            assertThrows(RuntimeException.class, () -> repository.updatePassword(ghost));
        }
    }

    /**
     * save() never wraps the shared Connection in try-with-resources -- only the
     * PreparedStatement is auto-closed -- so a SQLException mid-save must not leave
     * the thread-local connection itself in a broken state for later calls on the
     * same thread.
     */
    @Nested
    @DisplayName("Connection resilience after a mid-save SQLException")
    class ConnectionResilienceTests {

        @Test
        @DisplayName("save() with a duplicate username throws, without corrupting the connection")
        void duplicateUsernameThrowsButConnectionSurvives() {
            repository.save(newUser("nadia", Role.CUSTOMER));

            // username has a UNIQUE constraint -- this violates it and raises a
            // SQLException, wrapped as a RuntimeException by save().
            assertThrows(RuntimeException.class,
                    () -> repository.save(newUser("nadia", Role.CUSTOMER)));

            // The same thread's connection (unchanged by the failed save) must
            // still work for subsequent calls, both read and write.
            assertTrue(repository.existsByUsername("nadia"));
            assertDoesNotThrow(() -> repository.save(newUser("olga", Role.CUSTOMER)));
            assertTrue(repository.existsByUsername("olga"));
        }

        @Test
        @DisplayName("save() with a short username throws, without corrupting the connection")
        void shortUsernameThrowsButConnectionSurvives() {
            // username has a CHECK constraint -- this violates it and raises a
            // SQLException, wrapped as a RuntimeException by save().
            assertThrows(RuntimeException.class,
                    () -> repository.save(newUser("ab", Role.CUSTOMER)));
            // The same thread's connection (unchanged by the failed save) must
            // still work for subsequent calls, both read and write.
            assertDoesNotThrow(() -> repository.save(newUser("peter", Role.CUSTOMER)));
        }
    }

    /**
     * The failure-path tests above all go through constraint violations
     * (UNIQUE, CHECK). This covers the plain catch (SQLException e) branch
     * every read/write method falls back to for any other driver-level
     * failure -- e.g. the underlying table being gone.
     */
    @Nested
    @DisplayName("Generic database failure (not a constraint violation)")
    class DatabaseFailureTests {

        @Test
        @DisplayName("findByUsername() wraps a SQLException in a RuntimeException")
        void findByUsernameWrapsSqlExceptionInRuntimeException() {
            DatabaseUtil.execSQL("DROP TABLE users");

            assertThrows(RuntimeException.class, () -> repository.findByUsername("nadia"));
        }
    }
}

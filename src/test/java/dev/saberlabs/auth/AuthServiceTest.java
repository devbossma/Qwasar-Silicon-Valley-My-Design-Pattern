package dev.saberlabs.auth;

import dev.saberlabs.auth.repositories.UserRepository;
import dev.saberlabs.auth.repositories.implementations.memory.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuthService")
class AuthServiceTest {

    private UserRepository userRepository;
    private AuthService    authService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        authService = new AuthService(userRepository);
    }

    // ================================================================
    // Password hashing
    // ================================================================

    @Nested
    @DisplayName("Password hashing")
    class HashingTests {

        @Test
        @DisplayName("hashPassword produces a consistent hash for the same input")
        void consistentHash() {
            String hash1 = authService.hashPassword("mypassword");
            String hash2 = authService.hashPassword("mypassword");
            assertEquals(hash1, hash2);
        }

        @Test
        @DisplayName("hashPassword never returns the plain-text password")
        void neverReturnsPlainText() {
            String hash = authService.hashPassword("mypassword");
            assertNotEquals("mypassword", hash);
        }

        @Test
        @DisplayName("verifyPassword returns true for the correct password")
        void verifyCorrectPassword() {
            String hash = authService.hashPassword("secret123");
            assertTrue(authService.verifyPassword("secret123", hash));
        }

        @Test
        @DisplayName("verifyPassword returns false for an incorrect password")
        void verifyIncorrectPassword() {
            String hash = authService.hashPassword("secret123");
            assertFalse(authService.verifyPassword("wrongpassword", hash));
        }
    }

    // ================================================================
    // register()
    // ================================================================

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("registers a new CUSTOMER by default")
        void registersCustomerByDefault() {
            User user = authService.register("alice", "password123");
            assertEquals(Role.CUSTOMER, user.role());
        }

        @Test
        @DisplayName("password is hashed, never stored in plain text")
        void passwordIsHashed() {
            User user = authService.register("alice", "password123");
            assertNotEquals("password123", user.passwordHash());
        }

        @Test
        @DisplayName("rejects username shorter than 3 characters")
        void rejectsShortUsername() {
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register("ab", "password123"));
        }

        @Test
        @DisplayName("rejects username with invalid characters")
        void rejectsInvalidCharacters() {
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register("alice!", "password123"));
        }

        @Test
        @DisplayName("rejects password shorter than 6 characters")
        void rejectsShortPassword() {
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register("alice", "12345"));
        }

        @Test
        @DisplayName("rejects a duplicate username")
        void rejectsDuplicateUsername() {
            authService.register("alice", "password123");
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register("alice", "different456"));
        }

        @Test
        @DisplayName("allows underscores in username")
        void allowsUnderscores() {
            assertDoesNotThrow(() -> authService.register("alice_smith", "password123"));
        }

        @Test
        @DisplayName("rejects an empty username")
        void rejectsEmptyUsername() {
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register("", "password123"));
        }

        @Test
        @DisplayName("rejects an empty password")
        void rejectsEmptyPassword() {
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register("alice", ""));
        }

        @Test
        @DisplayName("rejects a blank (whitespace-only) username")
        void rejectsBlankUsername() {
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register("   ", "password123"));
        }

        @Test
        @DisplayName("rejects the reserved 'manager' username")
        void rejectsReservedManagerUsername() {
            assertThrows(IllegalArgumentException.class,
                    () -> authService.register("manager", "password123"));
        }

        @Test
        @DisplayName("rejects a null username")
        void rejectsNullUsername() {
            assertThrows(NullPointerException.class,
                    () -> authService.register(null, "password123"));
        }

        @Test
        @DisplayName("rejects a null password")
        void rejectsNullPassword() {
            assertThrows(NullPointerException.class,
                    () -> authService.register("alice", null));
        }
    }

    // ================================================================
    // login()
    // ================================================================

    @Nested
    @DisplayName("login()")
    class LoginTests {

        @Test
        @DisplayName("succeeds with correct credentials")
        void succeedsWithCorrectCredentials() {
            authService.register("alice", "password123");
            User user = authService.login("alice", "password123");
            assertEquals("alice", user.username());
        }

        @Test
        @DisplayName("throws AuthException for unknown username")
        void throwsForUnknownUsername() {
            assertThrows(AuthException.class,
                    () -> authService.login("ghost", "password123"));
        }

        @Test
        @DisplayName("throws AuthException for wrong password")
        void throwsForWrongPassword() {
            authService.register("alice", "password123");
            assertThrows(AuthException.class,
                    () -> authService.login("alice", "wrongpassword"));
        }

        @Test
        @DisplayName("rejects a null username")
        void rejectsNullUsername() {
            assertThrows(NullPointerException.class,
                    () -> authService.login(null, "password123"));
        }

        @Test
        @DisplayName("rejects a null password")
        void rejectsNullPassword() {
            authService.register("alice", "password123");
            assertThrows(NullPointerException.class,
                    () -> authService.login("alice", null));
        }
    }

    // ================================================================
    // seedManagerIfAbsent()
    // ================================================================

    @Nested
    @DisplayName("seedManagerIfAbsent()")
    class SeedManagerTests {

        @Test
        @DisplayName("creates a MANAGER account when no users exist")
        void createsManagerWhenEmpty() {
            authService.seedManagerIfAbsent();
            var managers = userRepository.findByRole(Role.MANAGER);
            assertEquals(1, managers.size());
        }

        @Test
        @DisplayName("does nothing when users already exist")
        void doesNothingWhenUsersExist() {
            authService.register("alice", "password123");
            authService.seedManagerIfAbsent();

            assertEquals(1, userRepository.findAll().size());
            assertTrue(userRepository.findByRole(Role.MANAGER).isEmpty());
        }

        @Test
        @DisplayName("seeded manager can log in with default credentials")
        void seededManagerCanLogin() {
            authService.seedManagerIfAbsent();
            assertDoesNotThrow(() -> authService.login("manager", "manager123"));
        }
    }

    // ================================================================
    // createBarista()
    // ================================================================

    @Nested
    @DisplayName("createBarista()")
    class CreateBaristaTests {

        @Test
        @DisplayName("manager can create a barista account")
        void managerCanCreateBarista() {
            User manager = authService.register("boss", "password123", Role.MANAGER);
            User barista = authService.createBarista(manager, "sara", "password123");
            assertEquals(Role.BARISTA, barista.role());
        }

        @Test
        @DisplayName("non-manager cannot create a barista account")
        void nonManagerCannotCreateBarista() {
            User customer = authService.register("alice", "password123");
            assertThrows(SecurityException.class,
                    () -> authService.createBarista(customer, "sara", "password123"));
        }
    }

    // ================================================================
    // deleteUser()
    // ================================================================

    @Nested
    @DisplayName("deleteUser()")
    class DeleteUserTests {

        @Test
        @DisplayName("manager can delete another user")
        void managerCanDeleteUser() {
            User manager = authService.register("boss", "password123", Role.MANAGER);
            User customer = authService.register("alice", "password123");

            assertDoesNotThrow(() -> authService.deleteUser(manager, customer.id()));
            assertTrue(userRepository.findById(customer.id()).isEmpty());
        }

        @Test
        @DisplayName("non-manager cannot delete a user")
        void nonManagerCannotDelete() {
            User customer = authService.register("alice", "password123");
            User other = authService.register("bob", "password123");

            assertThrows(SecurityException.class,
                    () -> authService.deleteUser(customer, other.id()));
        }

        @Test
        @DisplayName("manager cannot delete their own account")
        void managerCannotDeleteSelf() {
            User manager = authService.register("boss", "password123", Role.MANAGER);
            assertThrows(IllegalArgumentException.class,
                    () -> authService.deleteUser(manager, manager.id()));
        }

        @Test
        @DisplayName("throws when target user does not exist")
        void throwsWhenTargetMissing() {
            User manager = authService.register("boss", "password123", Role.MANAGER);
            assertThrows(IllegalArgumentException.class,
                    () -> authService.deleteUser(manager, 999L));
        }
    }

    // ================================================================
    // changePassword()
    // ================================================================

    @Nested
    @DisplayName("changePassword()")
    class ChangePasswordTests {

        @Test
        @DisplayName("succeeds with correct current password")
        void succeedsWithCorrectCurrentPassword() {
            User user = authService.register("alice", "oldpassword");
            assertDoesNotThrow(() ->
                    authService.changePassword(user, "oldpassword", "newpassword"));
        }

        @Test
        @DisplayName("new password allows login, old password no longer works")
        void newPasswordWorksOldDoesNot() {
            User user = authService.register("alice", "oldpassword");
            authService.changePassword(user, "oldpassword", "newpassword");

            assertDoesNotThrow(() -> authService.login("alice", "newpassword"));
            assertThrows(AuthException.class,
                    () -> authService.login("alice", "oldpassword"));
        }

        @Test
        @DisplayName("throws when current password is wrong")
        void throwsWhenCurrentPasswordWrong() {
            User user = authService.register("alice", "oldpassword");
            assertThrows(AuthException.class,
                    () -> authService.changePassword(user, "wrongpassword", "newpassword"));
        }

        @Test
        @DisplayName("throws when new password is too short")
        void throwsWhenNewPasswordTooShort() {
            User user = authService.register("alice", "oldpassword");
            assertThrows(IllegalArgumentException.class,
                    () -> authService.changePassword(user, "oldpassword", "short"));
        }
    }
}
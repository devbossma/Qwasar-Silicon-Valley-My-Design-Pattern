package dev.saberlabs.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Peer review flagged User.toString()/hasRole() as sitting at 0% coverage --
 * this class closes that gap directly rather than relying on incidental
 * exercise through repository/service tests.
 */
@DisplayName("User")
class UserTest {

    private User newUser(Role role) {
        return new User(1L, "alice", "hashed-password", role, LocalDateTime.now());
    }

    @Nested
    @DisplayName("compact constructor")
    class CompactConstructorTests {

        @Test
        @DisplayName("rejects a blank username")
        void rejectsBlankUsername() {
            assertThrows(IllegalArgumentException.class,
                    () -> new User(1L, "   ", "hashed-password", Role.CUSTOMER, LocalDateTime.now()));
        }

        @Test
        @DisplayName("rejects a blank password hash")
        void rejectsBlankPasswordHash() {
            assertThrows(IllegalArgumentException.class,
                    () -> new User(1L, "alice", "   ", Role.CUSTOMER, LocalDateTime.now()));
        }
    }

    @Nested
    @DisplayName("hasRole()")
    class HasRoleTests {

        @Test
        @DisplayName("true when the role matches")
        void trueWhenRoleMatches() {
            assertTrue(newUser(Role.BARISTA).hasRole(Role.BARISTA));
        }

        @Test
        @DisplayName("false when the role differs")
        void falseWhenRoleDiffers() {
            assertFalse(newUser(Role.BARISTA).hasRole(Role.CUSTOMER));
        }
    }

    @Nested
    @DisplayName("role predicates")
    class RolePredicateTests {

        @Test
        @DisplayName("isManager() true only for MANAGER")
        void isManager() {
            assertTrue(newUser(Role.MANAGER).isManager());
            assertFalse(newUser(Role.CUSTOMER).isManager());
        }

        @Test
        @DisplayName("isBarista() true only for BARISTA")
        void isBarista() {
            assertTrue(newUser(Role.BARISTA).isBarista());
            assertFalse(newUser(Role.CUSTOMER).isBarista());
        }

        @Test
        @DisplayName("isCustomer() true only for CUSTOMER")
        void isCustomer() {
            assertTrue(newUser(Role.CUSTOMER).isCustomer());
            assertFalse(newUser(Role.BARISTA).isCustomer());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("includes id, username and role, but never the password hash")
        void formatsWithoutExposingPasswordHash() {
            User user = newUser(Role.CUSTOMER);

            String result = user.toString();

            assertTrue(result.contains("alice"));
            assertTrue(result.contains("CUSTOMER"));
            assertTrue(result.contains("1"));
            assertFalse(result.contains("hashed-password"));
        }
    }

    @Test
    @DisplayName("getId() returns the same value as id()")
    void getIdMatchesRecordAccessor() {
        User user = newUser(Role.CUSTOMER);

        assertEquals(user.id(), user.getId());
    }
}

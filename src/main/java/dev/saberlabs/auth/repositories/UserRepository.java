package dev.saberlabs.auth.repositories;

import dev.saberlabs.auth.Role;
import dev.saberlabs.auth.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

/**
 * Repository abstraction for persisted User accounts.
 */
public interface UserRepository {

    @NotNull User save(@NotNull User user);

    @NotNull Optional<User> findByUsername(@NotNull String username);

    @NotNull Optional<User> findById(long id);

    @NotNull List<User> findAll();

    @NotNull List<User> findByRole(@NotNull Role role);

    boolean deleteById(long id);

    boolean existsByUsername(@NotNull String username);

    void updatePassword(@NotNull User user);
}
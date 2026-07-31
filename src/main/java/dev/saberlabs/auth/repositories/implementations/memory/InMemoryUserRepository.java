package dev.saberlabs.auth.repositories.implementations.memory;

import dev.saberlabs.auth.Role;
import dev.saberlabs.auth.User;
import dev.saberlabs.auth.repositories.UserRepository;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryUserRepository implements UserRepository {

    private final List<User> users = new ArrayList<>();
    private final AtomicLong idCounter = new AtomicLong(0);

    @Override
    public synchronized @NotNull User save(@NotNull User user) {
        User saved = new User(idCounter.incrementAndGet(), user.username(),
                user.passwordHash(), user.role(), user.createdAt());
        users.add(saved);
        return saved;
    }

    @Override
    public synchronized @NotNull Optional<User> findByUsername(@NotNull String username) {
        return users.stream().filter(u -> u.username().equals(username)).findFirst();
    }

    @Override
    public synchronized @NotNull Optional<User> findById(long id) {
        return users.stream().filter(u -> u.id() == id).findFirst();
    }

    @Override
    public synchronized @NotNull List<User> findAll() {
        return List.copyOf(users);
    }

    @Override
    public synchronized @NotNull List<User> findByRole(@NotNull Role role) {
        return users.stream().filter(u -> u.role() == role).toList();
    }

    @Override
    public synchronized boolean deleteById(long id) {
        return users.removeIf(u -> u.id() == id);
    }

    @Override
    public synchronized boolean existsByUsername(@NotNull String username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public synchronized void updatePassword(@NotNull User user) {
        users.removeIf(u -> u.id() == user.id());
        users.add(user);
    }

    public synchronized void clear() {
        users.clear();
    }
}
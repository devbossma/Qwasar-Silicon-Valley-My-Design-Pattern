package dev.saberlabs.views;

import dev.saberlabs.auth.AuthService;
import dev.saberlabs.auth.Role;
import dev.saberlabs.auth.User;
import dev.saberlabs.auth.UserRepository;
import dev.saberlabs.chat.ChatService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Console-based dashboard for an authenticated MANAGER.
 *
 * Manages user accounts (create baristas, list/delete users) and can
 * inspect the full chat history across all customers.
 */
public class ManagerView {

    private static final String THIN_SEP =
            "────────────────────────────────────────────────────────";

    @NotNull private final User manager;
    @NotNull private final AuthService authService;
    @NotNull private final UserRepository userRepository;
    @NotNull private final ChatService chatService;
    @NotNull private final Scanner scanner;

    public ManagerView(@NotNull User manager,
                       @NotNull AuthService authService,
                       @NotNull UserRepository userRepository,
                       @NotNull ChatService chatService,
                       @NotNull Scanner scanner) {
        this.manager = Objects.requireNonNull(manager, "Manager cannot be null");
        this.authService = Objects.requireNonNull(authService, "AuthService cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.chatService = Objects.requireNonNull(chatService, "ChatService cannot be null");
        this.scanner = Objects.requireNonNull(scanner, "Scanner cannot be null");
    }

    public void run() {
        printWelcome();
        printHelp();

        boolean running = true;
        while (running) {
            System.out.print("\n[" + manager.username() + "] > ");
            String input = scanner.nextLine().trim();
            String[] parts = input.split("\\s+");
            String command = parts.length > 0 ? parts[0].toLowerCase() : "";

            switch (command) {
                case "quit", "exit" -> running = false;
                case "help" -> printHelp();
                case "create-barista" -> handleCreateBarista(parts);
                case "list-users" -> handleListUsers();
                case "delete-user" -> handleDeleteUser(parts);
                case "history" -> handleHistory();
                case "" -> { /* empty line, ignore */ }
                default -> System.out.println("  Unknown command. Type 'help'.");
            }
        }

        System.out.println("[" + manager.username() + "] Logged out.\n");
    }

    private void handleCreateBarista(String[] parts) {
        if (parts.length < 3) {
            System.out.println("  Usage: create-barista <username> <password>");
            return;
        }
        try {
            User barista = authService.createBarista(manager, parts[1], parts[2]);
            System.out.printf("  ✓ Barista account created: %s (ID: %d)%n",
                    barista.username(), barista.id());
        } catch (IllegalArgumentException | SecurityException e) {
            System.out.println("  ✗ " + e.getMessage());
        }
    }

    private void handleListUsers() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            System.out.println("  No users registered.");
            return;
        }
        System.out.println("  " + THIN_SEP);
        System.out.printf("  %-6s %-15s %-10s%n", "ID", "Username", "Role");
        System.out.println("  " + THIN_SEP);
        users.forEach(u -> System.out.printf("  %-6d %-15s %-10s%n",
                u.id(), u.username(), u.role()));
    }

    private void handleDeleteUser(String[] parts) {
        if (parts.length < 2) {
            System.out.println("  Usage: delete-user <id>");
            return;
        }
        try {
            long id = Long.parseLong(parts[1]);
            authService.deleteUser(manager, id);
            System.out.println("  ✓ User deleted.");
        } catch (NumberFormatException e) {
            System.out.println("  ✗ Invalid ID: " + parts[1]);
        } catch (IllegalArgumentException | SecurityException e) {
            System.out.println("  ✗ " + e.getMessage());
        }
    }

    private void handleHistory() {
        var history = chatService.loadHistory();
        if (history.isEmpty()) {
            System.out.println("  No chat history yet.");
            return;
        }
        System.out.println("  ── Full Chat History (" + history.size() + " messages) ──");
        history.forEach(m -> System.out.println("  " + m));
    }

    private void printWelcome() {
        System.out.println();
        System.out.println(THIN_SEP);
        System.out.printf("  ☕ Manager Dashboard — %s%n", manager.username());
        System.out.println(THIN_SEP);
    }

    private void printHelp() {
        System.out.println();
        System.out.println("  Commands:");
        System.out.println("    create-barista <username> <password>  create a barista account");
        System.out.println("    list-users                            show all users");
        System.out.println("    delete-user <id>                      remove a user account");
        System.out.println("    history                                show all chat history");
        System.out.println("    help                                   show this help message");
        System.out.println("    quit                                   log out");
    }
}
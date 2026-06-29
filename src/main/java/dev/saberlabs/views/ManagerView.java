package dev.saberlabs.views;

import dev.saberlabs.auth.AuthService;
import dev.saberlabs.auth.User;
import dev.saberlabs.auth.repositories.UserRepository;
import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.ChatService;
import dev.saberlabs.chat.ChatSession;
import dev.saberlabs.chat.repositories.ChatRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Console-based dashboard for an authenticated MANAGER.
 *
 * Manages user accounts (create baristas, list/delete users) and can
 * inspect the full chat history across all sessions, or drill into a
 * single session's conversation.
 */
public class ManagerView {

    private static final String THIN_SEP =
            "────────────────────────────────────────────────────────";

    @NotNull private final User manager;
    @NotNull private final AuthService authService;
    @NotNull private final UserRepository userRepository;
    @NotNull private final ChatService chatService;
    @NotNull private final ChatRepository chatRepository;
    @NotNull private final Scanner scanner;

    public ManagerView(@NotNull User manager,
                       @NotNull AuthService authService,
                       @NotNull UserRepository userRepository,
                       @NotNull ChatService chatService,
                       @NotNull ChatRepository chatRepository,
                       @NotNull Scanner scanner) {
        this.manager = Objects.requireNonNull(manager, "Manager cannot be null");
        this.authService = Objects.requireNonNull(authService, "AuthService cannot be null");
        this.userRepository = Objects.requireNonNull(userRepository, "UserRepository cannot be null");
        this.chatService = Objects.requireNonNull(chatService, "ChatService cannot be null");
        this.chatRepository = Objects.requireNonNull(chatRepository, "ChatRepository cannot be null");
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
                case "sessions" -> printSessionDashboard();
                case "history" -> handleSessionHistory(parts);
                case "all-messages" -> handleAllMessages();
                case "" -> { /* empty line, ignore */ }
                default -> System.out.println("  Unknown command. Type 'help'.");
            }
        }

        System.out.println("[" + manager.username() + "] Logged out.\n");
    }

    // ================================================================
    // User management
    // ================================================================

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

    // ================================================================
    // Session and chat oversight
    // ================================================================

    /**
     * Shows every session in the system, its status, and who's assigned —
     * the manager's bird's-eye view across all customers and baristas.
     */
    private void printSessionDashboard() {
        List<ChatSession> all = chatService.getAllSessions();
        System.out.println();
        System.out.println("  ── All Sessions ──");
        if (all.isEmpty()) {
            System.out.println("  No sessions yet.");
            return;
        }
        System.out.printf("  %-6s %-12s %-10s %-12s%n", "ID", "Customer", "Status", "Barista");
        System.out.println("  " + THIN_SEP);
        for (ChatSession s : all) {
            String barista = s.baristaId() == null ? "—" : "Barista #" + s.baristaId();
            System.out.printf("  %-6d %-12d %-10s %-12s%n",
                    s.id(), s.customerId(), s.status(), barista);
        }
    }

    /**
     * Shows the full conversation for a single session by ID.
     * Usage: history <session-id>
     */
    private void handleSessionHistory(String[] parts) {
        if (parts.length < 2) {
            System.out.println("  Usage: history <session-id>");
            return;
        }
        long sessionId;
        try {
            sessionId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            System.out.println("  Invalid session ID: " + parts[1]);
            return;
        }

        List<ChatMessage> history = chatService.loadHistory(sessionId);
        if (history.isEmpty()) {
            System.out.println("  No messages found for session #" + sessionId);
            return;
        }
        System.out.println("  ── Session #" + sessionId + " ──");
        history.forEach(m -> System.out.println("  " + m));
    }

    /**
     * Shows every message ever sent, across all sessions, newest first —
     * a flat global audit log rather than a per-session view.
     */
    private void handleAllMessages() {
        List<ChatMessage> all = chatRepository.findAll();
        if (all.isEmpty()) {
            System.out.println("  No chat history yet.");
            return;
        }
        System.out.println("  ── All Messages (" + all.size() + " total) ──");
        all.forEach(m -> System.out.printf("  [Session #%d] %s%n", m.sessionId(), m));
    }

    // ================================================================
    // Display helpers
    // ================================================================

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
        System.out.println("    sessions                              show all chat sessions + status");
        System.out.println("    history <session-id>                  show one session's conversation");
        System.out.println("    all-messages                          show every message, all sessions");
        System.out.println("    help                                  show this help message");
        System.out.println("    quit                                  log out");
    }
}
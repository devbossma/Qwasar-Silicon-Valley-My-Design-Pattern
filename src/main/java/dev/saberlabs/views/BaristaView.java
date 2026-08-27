package dev.saberlabs.views;

import dev.saberlabs.auth.User;
import dev.saberlabs.chat.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Console view for an authenticated BARISTA — the "waiter" role.
 *
 * Goes READY on entry (joining the matching pool), can see all sessions
 * and who's responsible for each, can switch between their own multiple
 * ACTIVE sessions, chat within them, manually send orders to the kitchen,
 * and end sessions.
 */
public class BaristaView implements ChatObserver, NotificationObserver {


    private static final String THIN_SEP =
            "────────────────────────────────────────────────────────";

    @NotNull private final User user;
    @NotNull private final ChatService chatService;
    @NotNull private final ChatNotificationService notificationService;
    @NotNull private final Scanner scanner;
    private ChatSession activeSession;

    public BaristaView(@NotNull User user,
                       @NotNull ChatService chatService,
                       @NotNull ChatNotificationService notificationService,
                       @NotNull Scanner scanner) {
        this.user                = Objects.requireNonNull(user);
        this.chatService         = Objects.requireNonNull(chatService);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.scanner             = Objects.requireNonNull(scanner);
    }

    public void run() {
        chatService.registerObserver(this);
        notificationService.registerObserver(this);

        printWelcome();

        // Show unread notifications from previous sessions
        var unread = notificationService.getUnread(user.id());
        if (!unread.isEmpty()) {
            System.out.println("  ── Missed Notifications ──");
            unread.forEach(n -> System.out.println("  " + n));
            notificationService.markAllRead(user.id());
        }

        var matched = chatService.baristaReady(user);
        matched.ifPresent(s -> System.out.printf(
                "  ✓ Immediately matched with Session #%d%n", s.id()));

        printDashboard();
        printHelp();

        boolean running = true;
        while (running) {
            System.out.print("\n[" + user.username() + "] > ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            String[] parts   = input.split("\\s+");
            String   command = parts[0].toLowerCase();

            switch (command) {
                case "quit", "exit"      -> running = false;
                case "help"              -> printHelp();
                case "dashboard"         -> printDashboard();
                case "switch"            -> handleSwitch(parts);
                case "send-to-kitchen"   -> handleSendToKitchen(parts);
                case "end"               -> handleEndSession();
                case "back"              -> activeSession = null;
                default                  -> handleChatOrReply(input);
            }
        }

        chatService.baristaOffline(user);
        chatService.removeObserver(this);
        notificationService.removeObserver(this);
        System.out.println("[" + user.username() + "] Clocking out.\n");
    }

    // ================================================================
    // ChatObserver
    // ================================================================

    @Override
    public void onMessageReceived(@NotNull ChatMessage message) {
        if (activeSession == null || message.sessionId() != activeSession.id()) return;
        if (message.senderId() == user.id()) return;

        System.out.println();
        System.out.println("  " + message);
        System.out.print("[" + user.username() + "] > ");
    }

    @Override
    public void onNotificationReceived(@NotNull ChatNotification notification) {
        // Only display if this notification targets the current barista
        if (notification.userId() != user.id()) return;

        System.out.println();
        System.out.println("  " + notification);
        System.out.print("[" + user.username() + "] > ");
    }

    // ================================================================
    // Commands
    // ================================================================

    private void handleSwitch(String[] parts) {
        if (parts.length < 2) {
            System.out.println("  Usage: switch <session-id>");
            return;
        }
        long sessionId;
        try {
            sessionId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            System.out.println("  Invalid session ID.");
            return;
        }

        boolean owns = chatService.getActiveSessionsForBarista(user).stream()
                .anyMatch(s -> s.id() == sessionId);
        if (!owns) {
            System.out.println("  You are not assigned to session #" + sessionId);
            return;
        }

        activeSession = chatService.getActiveSessionsForBarista(user).stream()
                .filter(s -> s.id() == sessionId)
                .findFirst()
                .orElseThrow();

        System.out.printf("  ✓ Switched to session #%d%n", sessionId);
        printSessionHistory(sessionId);

        var pending = chatService.getPendingOrdersForSession(activeSession);
        if (!pending.isEmpty()) {
            System.out.printf("%n  ⚠ %d order(s) waiting to be sent to the kitchen — " +
                    "type 'send-to-kitchen' to review.%n", pending.size());
        }
    }

    private void handleChatOrReply(@NotNull String input) {
        if (activeSession == null) {
            System.out.println("  No active session selected. Use 'switch <id>' first.");
            return;
        }
        chatService.sendMessage(activeSession.id(), user.id(), user.username(), input);
    }

    private void handleSendToKitchen(String[] parts) {
        if (activeSession == null) {
            System.out.println("  No active session selected. Use 'switch <id>' first.");
            return;
        }

        var pending = chatService.getPendingOrdersForSession(activeSession);
        if (pending.isEmpty()) {
            System.out.println("  No pending orders for this customer.");
            return;
        }

        System.out.println();
        System.out.println("  ── Pending Orders (not yet sent to kitchen) ──");
        for (int i = 0; i < pending.size(); i++) {
            var order = pending.get(i);
            String description = order.baseCoffee()
                    + (order.extras().isEmpty() ? "" : " + " + String.join(" + ", order.extras()));
            System.out.printf("  %d. [%-8s] %-28s $%.2f  placed: %s%n",
                    i + 1,
                    order.id(),                                    // ← prominent ID
                    description,
                    order.total(),
                    order.createdAt().toLocalTime()                // ← timestamp distinguishes duplicates
                            .toString().substring(0, 5));
        }
        System.out.print("  Select order to send to kitchen (number, or 0 to cancel): ");

        String choice = scanner.nextLine().trim();
        int index;
        try {
            index = Integer.parseInt(choice);
        } catch (NumberFormatException e) {
            System.out.println("  Invalid selection.");
            return;
        }
        if (index == 0) {
            System.out.println("  Cancelled.");
            return;
        }
        if (index < 1 || index > pending.size()) {
            System.out.println("  Invalid selection.");
            return;
        }

        var selected = pending.get(index - 1);
        try {
            chatService.sendOrderToKitchen(activeSession, user.id(), selected.id());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("  Interrupted while sending order to kitchen.");
        }
    }

    private void handleEndSession() {
        if (activeSession == null) {
            System.out.println("  No active session selected. Use 'switch <id>' first.");
            return;
        }
        long endedId = activeSession.id();
        var rematch = chatService.endSession(endedId);
        System.out.println("  ✓ Session #" + endedId + " closed.");
        rematch.ifPresent(s -> System.out.println(
                "  → You were immediately rematched with Session #" + s.id()));
        activeSession = null;
    }

    // ================================================================
    // Display helpers
    // ================================================================

    private void printWelcome() {
        System.out.println();
        System.out.println(THIN_SEP);
        System.out.printf("  ☕ Barista %s on duty.%n", user.username());
        System.out.println(THIN_SEP);
    }

    private void printDashboard() {
        List<ChatSession> all = chatService.getAllSessions();
        System.out.println();
        System.out.println("  ── All Sessions ──");
        if (all.isEmpty()) {
            System.out.println("  No sessions yet.");
            return;
        }
        System.out.printf("  %-6s %-12s %-10s %-12s %-10s%n",
                "ID", "Customer", "Status", "Assigned To", "Pending");
        System.out.println("  " + THIN_SEP);
        for (ChatSession s : all) {
            String assigned = s.baristaId() == null ? "—"
                    : (s.baristaId() == user.id() ? "You" : "Barista #" + s.baristaId());
            int pendingCount = chatService.getPendingOrdersForSession(s).size();
            String pendingLabel = pendingCount == 0 ? "—" : pendingCount + " order(s)";
            System.out.printf("  %-6d %-12d %-10s %-12s %-10s%n",
                    s.id(), s.customerId(), s.status(), assigned, pendingLabel);
        }

        var queueSnapshot = chatService.getKitchenQueueSnapshot();
        if (queueSnapshot != null) {
            System.out.printf("%n  Kitchen queue: %d/%d orders waiting%n",
                    queueSnapshot.size(), queueSnapshot.capacity());
        }
    }

    private void printSessionHistory(long sessionId) {
        List<ChatMessage> history = chatService.loadHistory(sessionId);
        if (history.isEmpty()) {
            System.out.println("  No messages yet in this session.");
            return;
        }
        System.out.println("  ── Conversation ──");
        history.forEach(m -> System.out.println("  " + m));
    }

    private void printHelp() {
        System.out.println();
        System.out.println("  Commands:");
        System.out.println("    dashboard                  show all sessions and their status");
        System.out.println("    switch <session-id>        switch to one of YOUR active sessions");
        System.out.println("    send-to-kitchen <order-id> manually send an order for preparation");
        System.out.println("    end                        end the current session");
        System.out.println("    back                       deselect the current session");
        System.out.println("    help                       show this help message");
        System.out.println("    quit                       clock out");
        System.out.println("  Anything else is sent as a chat reply in the current session.");
    }
}
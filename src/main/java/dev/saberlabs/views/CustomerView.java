package dev.saberlabs.views;

import dev.saberlabs.auth.AuthException;
import dev.saberlabs.auth.AuthService;
import dev.saberlabs.auth.User;
import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.ChatObserver;
import dev.saberlabs.chat.ChatService;
import dev.saberlabs.chat.ChatSession;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Console view for an authenticated CUSTOMER — the four-option menu:
 * Start Chat, My Order History, My Info, Quit.
 */
public class CustomerView implements ChatObserver {

    private static final String THIN_SEP =
            "────────────────────────────────────────────────────────";

    @NotNull private final User user;
    @NotNull private final ChatService chatService;
    @NotNull private final AuthService authService;
    @NotNull private final Scanner scanner;

    /** The session currently displayed in the chat sub-loop, if any. */
    private ChatSession activeSession;

    public CustomerView(@NotNull User user,
                        @NotNull ChatService chatService,
                        @NotNull AuthService authService,
                        @NotNull Scanner scanner) {
        this.user = Objects.requireNonNull(user, "User cannot be null");
        this.chatService = Objects.requireNonNull(chatService, "ChatService cannot be null");
        this.authService = Objects.requireNonNull(authService, "AuthService cannot be null");
        this.scanner = Objects.requireNonNull(scanner, "Scanner cannot be null");
    }

    /**
     * Runs the customer's main menu loop until they choose Quit.
     */
    public void run() {
        chatService.resolveCustomer(user); // ensure Customer profile exists

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> runChatSubLoop();
                case "2" -> printOrderHistory();
                case "3" -> runMyInfoMenu();
                case "4" -> running = false;
                default -> System.out.println("  Invalid choice.\n");
            }
        }
        System.out.println("[" + user.username() + "] Goodbye!\n");
    }

    // ================================================================
    // 1. Start Chat
    // ================================================================

    private void runChatSubLoop() {
        activeSession = chatService.startChat(user);
        chatService.registerObserver(this);

        System.out.println();
        System.out.println(THIN_SEP);
        if (activeSession.isWaiting()) {
            System.out.println("  You're in the queue — waiting for the next available barista...");
        } else {
            System.out.printf("  Connected! Barista ID: %d%n", activeSession.baristaId());
        }
        System.out.println(THIN_SEP);

        printSessionHistory(activeSession.id());
        printChatHelp();

        boolean inChat = true;
        while (inChat) {
            System.out.print("\n[" + user.username() + "] > ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;

            switch (input.toLowerCase()) {
                case "end chat", "leave" -> {
                    chatService.endSession(activeSession.id());
                    System.out.println("  Chat session ended.\n");
                    inChat = false;
                }
                case "back" -> {
                    chatService.sendMessage(activeSession.id(), 0, "System",
                            user.username() + " has left the conversation (still reachable).");
                    inChat = false;
                } // leaves the view without ending the session
                case "help" -> printChatHelp();
                case "history" -> printSessionHistory(activeSession.id());
                default -> chatService.processCustomerInput(user, activeSession, input);
            }
        }

        chatService.removeObserver(this);
        activeSession = null;
    }

    @Override
    public void onMessageReceived(@NotNull ChatMessage message) {
        if (activeSession == null || message.sessionId() != activeSession.id()) return;
        if (message.senderId() == user.id()) return; // don't echo own messages

        System.out.println();
        System.out.println(message);
        System.out.print("[" + user.username() + "] > ");
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

    private void printChatHelp() {
        System.out.println();
        System.out.println("  Commands:");
        System.out.println("    order <coffee> [extras]   e.g. order cappuccino milk sugar");
        System.out.println("    history                   show this session's messages");
        System.out.println("    back                      return to menu (session stays open)");
        System.out.println("    end chat                  close this session");
        System.out.println("    help                      show this help message");
        System.out.println("  Available coffees: espresso, cappuccino, latte");
        System.out.println("  Available extras:  milk, sugar, whipped");
    }

    // ================================================================
    // 2. My Order History
    // ================================================================

    private void printOrderHistory() {
        var customer = chatService.resolveCustomer(user);
        var orders = chatService.getOrderHistory(user);

        System.out.println();
        if (orders.isEmpty()) {
            System.out.println("  You have no orders yet.\n");
            return;
        }

        System.out.printf("  ── Order History — %s, %d total ──%n",
                customer.getLoyaltyTier(), customer.getTotalOrders());
        System.out.println("  " + THIN_SEP);
        System.out.printf("  %-10s %-30s %-10s %-10s%n",
                "Order #", "Coffee", "Price", "Status");
        System.out.println("  " + THIN_SEP);
        orders.forEach(o -> {
            String description = o.baseCoffee()
                    + (o.extras().isEmpty() ? "" : " + " + String.join(" + ", o.extras()));
            System.out.printf("  %-10s %-30s $%-9.2f %-10s%n",
                    o.id(), description, o.total(), o.status());
        });
        System.out.println();
    }

    // ================================================================
    // 3. My Info
    // ================================================================

    private void runMyInfoMenu() {
        var customer = chatService.resolveCustomer(user);

        System.out.println();
        System.out.println("  ── My Info ──");
        System.out.println("  Username:      " + user.username());
        System.out.println("  Loyalty Tier:  " + customer.getLoyaltyTier());
        System.out.println("  Total Orders:  " + customer.getTotalOrders());
        System.out.println();
        System.out.println("  1. Change password");
        System.out.println("  2. Back");
        System.out.print("\n  Your choice: ");

        String choice = scanner.nextLine().trim();
        if (choice.equals("1")) {
            changePassword();
        }
    }

    private void changePassword() {
        System.out.print("  Current password: ");
        String current = scanner.nextLine().trim();
        System.out.print("  New password (min 6 chars): ");
        String newPassword = scanner.nextLine().trim();

        try {
            authService.changePassword(user, current, newPassword);
            System.out.println("  ✓ Password changed.\n");
        } catch (AuthException | IllegalArgumentException e) {
            System.out.println("  ✗ " + e.getMessage() + "\n");
        }
    }

    // ================================================================
    // Menu display
    // ================================================================

    private void printMenu() {
        System.out.println();
        System.out.println(THIN_SEP);
        System.out.printf("  ☕ Welcome, %s!%n", user.username());
        System.out.println(THIN_SEP);
        System.out.println("  1. Start Chat");
        System.out.println("  2. My Order History");
        System.out.println("  3. My Info");
        System.out.println("  4. Quit");
        System.out.print("\n  Your choice: ");
    }
}
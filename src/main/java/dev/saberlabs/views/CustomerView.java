package dev.saberlabs.views;

import dev.saberlabs.auth.User;
import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.ChatObserver;
import dev.saberlabs.chat.ChatService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Scanner;

/**
 * Console-based chat view for an authenticated CUSTOMER.
 *
 * Displays conversation history on entry, then accepts a stream of
 * commands: plain chat messages, "order &lt;coffee&gt; [extras]", or
 * "history" / "quit".
 *
 * Registers itself as a {@link ChatObserver} so it sees messages from
 * baristas in real time (e.g. order-ready notifications) while it's
 * blocked waiting on the next line of console input — those messages
 * print immediately above the next prompt.
 */
public class CustomerView implements ChatObserver {

    private static final String THIN_SEP =
            "────────────────────────────────────────────────────────";

    @NotNull private final User user;
    @NotNull private final ChatService chatService;
    @NotNull private final Scanner scanner;

    public CustomerView(@NotNull User user,
                        @NotNull ChatService chatService,
                        @NotNull Scanner scanner) {
        this.user = Objects.requireNonNull(user, "User cannot be null");
        this.chatService = Objects.requireNonNull(chatService, "ChatService cannot be null");
        this.scanner = Objects.requireNonNull(scanner, "Scanner cannot be null");
    }

    /**
     * Runs the customer chat loop until the user types "quit".
     */
    public void run() {
        chatService.registerObserver(this);
        // Resolve/create the linked Customer profile up front
        chatService.resolveCustomer(user);

        printWelcome();
        printHistory(chatService.loadHistory());
        printHelp();

        boolean running = true;
        while (running) {
            System.out.print("\n[" + user.username() + "] > ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            String lower = input.toLowerCase();
            switch (lower) {
                case "quit", "exit" -> running = false;
                case "history" -> printHistory(chatService.loadHistory());
                case "help" -> printHelp();
                default -> chatService.processInput(user, input);
            }
        }

        chatService.removeObserver(this);
        System.out.println("[" + user.username() + "] Goodbye!\n");
    }

    // ================================================================
    // ChatObserver — real-time messages (e.g. barista replies)
    // ================================================================

    @Override
    public void onMessageReceived(@NotNull ChatMessage message) {
        // Don't re-print the customer's own messages — processInput already
        // triggers this notification, and the customer just typed it.
        if (message.senderId() == user.id()) return;

        System.out.println();
        System.out.println(message);
        System.out.print("[" + user.username() + "] > ");
    }

    // ================================================================
    // Display helpers
    // ================================================================

    private void printWelcome() {
        System.out.println();
        System.out.println(THIN_SEP);
        System.out.printf("  ☕ Welcome back, %s!%n", user.username());
        System.out.println(THIN_SEP);
    }

    private void printHistory(@NotNull List<ChatMessage> history) {
        if (history.isEmpty()) {
            System.out.println("  No previous conversation history.");
            return;
        }
        System.out.println("  ── Conversation History ──");
        history.forEach(m -> System.out.println("  " + m));
    }

    private void printHelp() {
        System.out.println();
        System.out.println("  Commands:");
        System.out.println("    order <coffee> [extras]   e.g. order cappuccino milk sugar");
        System.out.println("    history                   show full conversation history");
        System.out.println("    help                      show this help message");
        System.out.println("    quit                      leave the chat");
        System.out.println("  Available coffees: espresso, cappuccino, latte");
        System.out.println("  Available extras:  milk, sugar, whipped");
    }
}
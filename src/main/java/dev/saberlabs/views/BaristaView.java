package dev.saberlabs.views;

import dev.saberlabs.auth.User;
import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.ChatObserver;
import dev.saberlabs.chat.ChatService;
import dev.saberlabs.singleton.CoffeeShop;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Scanner;

/**
 * Console-based chat view for an authenticated BARISTA.
 *
 * Shows incoming order messages as they happen and lets the barista
 * send free-text replies back into the shared chat. The actual coffee
 * preparation still runs on the existing {@link dev.saberlabs.multithread.Barista}
 * threads — this view is the human-facing chat surface only.
 */
public class BaristaView implements ChatObserver {

    private static final String THIN_SEP = "────────────────────────────────────────────────────────";

    @NotNull private final User user;
    @NotNull private final ChatService chatService;
    @NotNull private final CoffeeShop coffeeShop;
    @NotNull private final Scanner scanner;

    public BaristaView(@NotNull User user,
                       @NotNull ChatService chatService,
                       @NotNull CoffeeShop coffeeShop,
                       @NotNull Scanner scanner) {
        this.user = Objects.requireNonNull(user, "User cannot be null");
        this.chatService = Objects.requireNonNull(chatService, "ChatService cannot be null");
        this.coffeeShop = Objects.requireNonNull(coffeeShop, "CoffeeShop cannot be null");
        this.scanner = Objects.requireNonNull(scanner, "Scanner cannot be null");
    }

    public void run() {
        chatService.registerObserver(this);

        printWelcome();
        printQueueStatus();
        printHelp();

        boolean running = true;
        while (running) {
            System.out.print("\n[" + user.username() + "] > ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) continue;

            String lower = input.toLowerCase();
            switch (lower) {
                case "quit", "exit" -> running = false;
                case "status" -> printQueueStatus();
                case "help" -> printHelp();
                default -> chatService.sendMessage(user.id(), user.username(), input);
            }
        }

        chatService.removeObserver(this);
        System.out.println("[" + user.username() + "] Clocking out.\n");
    }

    @Override
    public void onMessageReceived(@NotNull ChatMessage message) {
        if (message.senderId() == user.id()) return;

        System.out.println();
        System.out.println(message);
        System.out.print("[" + user.username() + "] > ");
    }

    private void printWelcome() {
        System.out.println();
        System.out.println(THIN_SEP);
        System.out.printf("  ☕ Barista %s on duty.%n", user.username());
        System.out.println(THIN_SEP);
    }

    private void printQueueStatus() {
        var queue = coffeeShop.getOrderQueue();
        if (queue == null) {
            System.out.println("  Shop is not open yet — no queue active.");
            return;
        }
        System.out.printf("  Orders waiting: %d/%d%n", queue.size(), queue.getCapacity());
        coffeeShop.getBaristas().forEach(b ->
                System.out.printf("  %-12s completed: %d%n",
                        b.getName(), b.getOrdersCompleted()));
    }

    private void printHelp() {
        System.out.println();
        System.out.println("  Commands:");
        System.out.println("    status        show queue and barista stats");
        System.out.println("    help          show this help message");
        System.out.println("    quit          clock out");
        System.out.println("  Anything else is sent as a chat message to customers.");
    }
}
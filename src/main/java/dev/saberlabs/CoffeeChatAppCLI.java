package dev.saberlabs;

import dev.saberlabs.auth.AuthService;
import dev.saberlabs.auth.User;
import dev.saberlabs.auth.repositories.UserRepository;
import dev.saberlabs.auth.repositories.implementations.sqlite.SqliteUserRepository;
import dev.saberlabs.chat.BaristaQueue;
import dev.saberlabs.chat.ChatNotificationService;
import dev.saberlabs.chat.ChatService;
import dev.saberlabs.chat.repositories.ChatNotificationRepository;
import dev.saberlabs.chat.repositories.ChatOrderRepository;
import dev.saberlabs.chat.repositories.ChatRepository;
import dev.saberlabs.chat.repositories.ChatSessionRepository;
import dev.saberlabs.chat.repositories.implementations.sqlite.SqliteChatNotificationRepository;
import dev.saberlabs.chat.repositories.implementations.sqlite.SqliteChatOrderRepository;
import dev.saberlabs.chat.repositories.implementations.sqlite.SqliteChatRepository;
import dev.saberlabs.chat.repositories.implementations.sqlite.SqliteChatSessionRepository;
import dev.saberlabs.db.DatabaseUtil;
import dev.saberlabs.order.PersistingOrderObserver;
import dev.saberlabs.singleton.CoffeeShop;
import dev.saberlabs.views.BaristaView;
import dev.saberlabs.views.CustomerView;
import dev.saberlabs.views.LoginView;
import dev.saberlabs.views.ManagerView;

import java.util.Scanner;

/**
 * Entry point for Part 01 — Coffee Chat (console version).
 * *
 * Startup sequence:
 * 1.  Initialize SQLite database from schema.sql
 * 2.  Seed default MANAGER account if no users exist
 * 3.  Build repositories (all interface-typed, Sqlite-backed)
 * 4.  Build services (AuthService, ChatNotificationService, ChatService)
 * 5.  Open the coffee shop (starts worker Barista threads)
 * 6.  Register PersistingOrderObserver (mirrors Order status → DB + notifications)
 * 7.  Recover BaristaQueue state from persisted sessions
 * 8.  Login → route by role → view loop
 * 9.  Graceful shutdown
 */
public class CoffeeChatAppCLI {

    private static final int QUEUE_CAPACITY    = 10;
    private static final int NUMBER_OF_BARISTAS = 2;

    public static void main(String[] args) {

        // ── 1. Database ──────────────────────────────────────────────
        DatabaseUtil.initialize();

        // ── 2. Repositories ─────────────────────────────────────────
        UserRepository             userRepository         = new SqliteUserRepository();
        ChatRepository             chatRepository         = new SqliteChatRepository();
        ChatSessionRepository      sessionRepository      = new SqliteChatSessionRepository();
        ChatOrderRepository        orderRepository        = new SqliteChatOrderRepository();
        ChatNotificationRepository notificationRepository = new SqliteChatNotificationRepository();

        // ── 3. Auth ──────────────────────────────────────────────────
        AuthService authService = new AuthService(userRepository);
        authService.seedManagerIfAbsent();

        // ── 4. Services ──────────────────────────────────────────────
        ChatNotificationService notificationService =
                new ChatNotificationService(notificationRepository);

        BaristaQueue baristaQueue = new BaristaQueue();
        CoffeeShop   shop         = CoffeeShop.getInstance();

        ChatService chatService = new ChatService(
                chatRepository,
                sessionRepository,
                orderRepository,
                notificationService,
                baristaQueue,
                shop);

        // ── 5. Open shop ─────────────────────────────────────────────
        shop.open(QUEUE_CAPACITY, NUMBER_OF_BARISTAS);

        // ── 6. Register PersistingOrderObserver ──────────────────────
        // Mirrors every Order status transition (READY, FULFILLED) into:
        //   a) the orders table (via ChatOrderRepository)
        //   b) user-scoped notifications (via ChatNotificationService)
        shop.registerObserver(
                new PersistingOrderObserver(orderRepository, notificationService));

        // ── 7. Recover BaristaQueue from persisted sessions ──────────
        chatService.recoverSessionsOnStartup();

        // ── 8. Main loop ─────────────────────────────────────────────
        try (Scanner scanner = new Scanner(System.in)) {
            boolean keepRunning = true;
            while (keepRunning) {

                LoginView loginView = new LoginView(authService, scanner);
                User user = loginView.run();

                switch (user.role()) {
                    case CUSTOMER -> new CustomerView(
                            user, chatService, notificationService,
                            authService, scanner).run();

                    case BARISTA -> new BaristaView(
                            user, chatService, notificationService,
                            shop, scanner).run();

                    case MANAGER -> new ManagerView(
                            user, authService, userRepository,
                            chatService, chatRepository, scanner).run();
                }

                System.out.print("\nReturn to login screen? (y/n): ");
                keepRunning = scanner.nextLine().trim().equalsIgnoreCase("y");
            }
        } finally {
            System.out.println("\nShutting down Coffee Chat...");
            shop.close();
            DatabaseUtil.closeConnection();
            System.out.println("Goodbye!");
        }
    }
}
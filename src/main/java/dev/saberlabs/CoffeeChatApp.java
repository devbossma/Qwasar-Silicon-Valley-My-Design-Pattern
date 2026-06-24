package dev.saberlabs;

import dev.saberlabs.auth.AuthService;
import dev.saberlabs.auth.User;
import dev.saberlabs.auth.UserRepository;
import dev.saberlabs.chat.BaristaQueue;
import dev.saberlabs.chat.ChatRepository;
import dev.saberlabs.chat.ChatService;
import dev.saberlabs.chat.ChatSessionCloser;
import dev.saberlabs.chat.ChatSessionRepository;
import dev.saberlabs.db.DatabaseUtil;
import dev.saberlabs.singleton.CoffeeShop;
import dev.saberlabs.views.BaristaView;
import dev.saberlabs.views.CustomerView;
import dev.saberlabs.views.LoginView;
import dev.saberlabs.views.ManagerView;

import java.util.Scanner;

/**
 * Entry point for Part 01 — Coffee Chat (console version).
 *
 * Startup sequence:
 * 1. Initialize the SQLite database from schema.sql (create tables if absent).
 * 2. Seed a default MANAGER account if no users exist yet.
 * 3. Open the coffee shop (starts the existing worker Barista threads).
 * 4. Recover BaristaQueue state from whatever sessions survived a restart.
 * 5. Register the ChatSessionCloser so fulfilled orders auto-close their session.
 * 6. Show the login screen and route by role until the user quits.
 * 7. On exit, close the shop and the database connection.
 */
public class CoffeeChatApp {

    private static final int QUEUE_CAPACITY = 10;
    private static final int NUMBER_OF_BARISTAS = 2;

    public static void main(String[] args) {
        // 1 & 2 — Database + seed manager
        DatabaseUtil.initialize();

        UserRepository userRepository = new UserRepository();
        AuthService authService = new AuthService(userRepository);
        authService.seedManagerIfAbsent();

        ChatRepository chatRepository = new ChatRepository();
        ChatSessionRepository sessionRepository = new ChatSessionRepository();
        BaristaQueue baristaQueue = new BaristaQueue();
        CoffeeShop shop = CoffeeShop.getInstance();
        ChatService chatService = new ChatService(
                chatRepository, sessionRepository, baristaQueue, shop);

        // 3 — Open the shop (worker Barista threads start consuming the OrderQueue)
        shop.open(QUEUE_CAPACITY, NUMBER_OF_BARISTAS);

        // 4 — Recover live queue state from persisted sessions
        chatService.recoverSessionsOnStartup();

        // 5 — Auto-close chat sessions when their order is fulfilled
        shop.registerObserver(new ChatSessionCloser(chatService, chatRepository));

        try (Scanner scanner = new Scanner(System.in)) {
            boolean keepRunning = true;
            while (keepRunning) {
                // 6 — Login screen, then route by role
                LoginView loginView = new LoginView(authService, scanner);
                User user = loginView.run();

                switch (user.role()) {
                    case CUSTOMER -> new CustomerView(user, chatService, scanner).run();
                    case BARISTA -> new BaristaView(user, chatService, shop, scanner).run();
                    case MANAGER -> new ManagerView(
                            user, authService, userRepository, chatService, scanner).run();
                }

                System.out.print("\nReturn to login screen? (y/n): ");
                keepRunning = scanner.nextLine().trim().equalsIgnoreCase("y");
            }
        } finally {
            // 7 — Graceful shutdown
            System.out.println("\nShutting down Coffee Chat...");
            shop.close();
            DatabaseUtil.closeConnection();
            System.out.println("Goodbye!");
        }
    }
}
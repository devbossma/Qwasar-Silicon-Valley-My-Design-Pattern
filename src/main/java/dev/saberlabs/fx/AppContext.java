package dev.saberlabs.fx;

import dev.saberlabs.CoffeeChatAppFX;
import dev.saberlabs.auth.AuthService;
import dev.saberlabs.auth.User;
import dev.saberlabs.chat.BaristaQueue;
import dev.saberlabs.chat.ChatNotificationService;
import dev.saberlabs.chat.ChatService;
import dev.saberlabs.chat.repositories.ChatImageRepository;
import dev.saberlabs.chat.repositories.ChatNotificationRepository;
import dev.saberlabs.chat.repositories.ChatOrderRepository;
import dev.saberlabs.chat.repositories.ChatRepository;
import dev.saberlabs.chat.repositories.ChatSessionRepository;
import dev.saberlabs.chat.repositories.implementations.sqlite.SqliteChatImageRepository;
import dev.saberlabs.chat.repositories.implementations.sqlite.SqliteChatNotificationRepository;
import dev.saberlabs.chat.repositories.implementations.sqlite.SqliteChatOrderRepository;
import dev.saberlabs.chat.repositories.implementations.sqlite.SqliteChatRepository;
import dev.saberlabs.chat.repositories.implementations.sqlite.SqliteChatSessionRepository;
import dev.saberlabs.auth.repositories.UserRepository;
import dev.saberlabs.auth.repositories.implementations.sqlite.SqliteUserRepository;
import dev.saberlabs.db.DatabaseUtil;
import dev.saberlabs.order.PersistingOrderObserver;
import dev.saberlabs.singleton.CoffeeShop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Application-scoped context holding all service and repository instances
 * for the JavaFX application. Initialized once in {@link CoffeeChatAppFX#start(javafx.stage.Stage)}
 * before any scene is loaded, then accessed statically by FXML controllers.
 *
 * JavaFX's FXMLLoader instantiates controllers via reflection (no-arg
 * constructor), so constructor injection is not practical. AppContext
 * provides a clean alternative — controllers call AppContext.getInstance()
 * rather than receiving dependencies through constructors.
 *
 * Lifetime: equal to the JVM process. Shutdown via {@link #shutdown()}.
 */
public class AppContext {

    private static volatile AppContext INSTANCE;

    // ── Repositories ─────────────────────────────────────────────────
    private final UserRepository            userRepository;
    private final ChatRepository            chatRepository;
    private final ChatSessionRepository     sessionRepository;
    private final ChatOrderRepository       orderRepository;
    private final ChatNotificationRepository notificationRepository;
    private final ChatImageRepository       imageRepository;

    // ── Services ─────────────────────────────────────────────────────
    private final AuthService               authService;
    private final ChatNotificationService   notificationService;
    private final ChatService               chatService;
    private final CoffeeShop               coffeeShop;

    // ── Session state ────────────────────────────────────────────────
    /** The user who successfully logged in — set by LoginController. */
    @Nullable private volatile User currentUser;

    private AppContext() {
        // ── 1. Database ──────────────────────────────────────────────
        DatabaseUtil.initialize();

        // ── 2. Repositories ─────────────────────────────────────────
        userRepository        = new SqliteUserRepository();
        chatRepository        = new SqliteChatRepository();
        sessionRepository     = new SqliteChatSessionRepository();
        orderRepository       = new SqliteChatOrderRepository();
        notificationRepository = new SqliteChatNotificationRepository();
        imageRepository       = new SqliteChatImageRepository();

        // ── 3. Services ──────────────────────────────────────────────
        authService           = new AuthService(userRepository);
        authService.seedManagerIfAbsent();

        notificationService   = new ChatNotificationService(notificationRepository);

        coffeeShop            = CoffeeShop.getInstance();
        BaristaQueue baristaQueue = new BaristaQueue();

        chatService = new ChatService(
                chatRepository,
                sessionRepository,
                orderRepository,
                notificationService,
                baristaQueue,
                coffeeShop);

        // ── 4. Open shop ─────────────────────────────────────────────
        coffeeShop.open(10, 2);

        // ── 5. Register PersistingOrderObserver ──────────────────────
        coffeeShop.registerObserver(
                new PersistingOrderObserver(orderRepository, notificationService));

        // ── 6. Recover BaristaQueue from persisted sessions ──────────
        chatService.recoverSessionsOnStartup();
    }

    /**
     * Returns the single AppContext instance, creating it on first call.
     * Thread-safe via double-checked locking.
     */
    public static @NotNull AppContext getInstance() {
        if (INSTANCE == null) {
            synchronized (AppContext.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppContext();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Graceful shutdown — closes the shop and database connection.
     * Called from {@link CoffeeChatAppFX#stop()}.
     */
    public void shutdown() {
        coffeeShop.close();
        DatabaseUtil.closeConnection();
    }

    // ── Getters ──────────────────────────────────────────────────────

    public @NotNull AuthService getAuthService() {
        return authService;
    }

    public @NotNull ChatService getChatService() {
        return chatService;
    }

    public @NotNull ChatNotificationService getNotificationService() {
        return notificationService;
    }

    public @NotNull CoffeeShop getCoffeeShop() {
        return coffeeShop;
    }

    public @NotNull UserRepository getUserRepository() {
        return userRepository;
    }

    public @NotNull ChatRepository getChatRepository() {
        return chatRepository;
    }

    public @NotNull ChatImageRepository getImageRepository() {
        return imageRepository;
    }

    public @Nullable User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(@Nullable User user) {
        this.currentUser = user;
    }

    public @NotNull ChatOrderRepository getOrderRepository() {
        return orderRepository;
    }
}
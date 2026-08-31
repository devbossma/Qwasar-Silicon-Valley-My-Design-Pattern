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
import dev.saberlabs.facade.CoffeeShopFacade;
import dev.saberlabs.order.OrderService;
import dev.saberlabs.order.PersistingOrderObserver;
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
    private final CoffeeShopFacade          coffeeShopFacade;

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

        BaristaQueue baristaQueue = new BaristaQueue();

        // OrderService is the FX app's only door onto the CoffeeShop singleton. No payment
        // gateway needed here: this shared instance only ever places orders; payment is still
        // collected per-order via ChatService.collectPaymentAndFulfill's own gateway.
        OrderService orderService = new OrderService();

        chatService = new ChatService(
                chatRepository,
                sessionRepository,
                orderRepository,
                notificationService,
                baristaQueue,
                orderService);

        // CoffeeShopFacade composes the same OrderService the reflection framework's real
        // BusinessObject (dev.saberlabs.chat.CoffeeShopBusiness, owned by ChatService) also
        // places orders through — one Command/OrderInvoker pipeline either way.
        coffeeShopFacade = new CoffeeShopFacade(orderService);

        // ── 4. Open shop ─────────────────────────────────────────────
        coffeeShopFacade.open(10, 2);

        // ── 5. Register PersistingOrderObserver ──────────────────────
        coffeeShopFacade.registerCustomer(
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
        coffeeShopFacade.close();
        DatabaseUtil.closeAllConnections();
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
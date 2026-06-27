package dev.saberlabs.chat;

import dev.saberlabs.auth.User;
import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.factory.LatteCreator;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.singleton.CoffeeShop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Coordinates the full chat experience: barista-pool session matching,
 * message persistence, order placement via chat commands, and Observer
 * notifications to console/UI listeners.
 *
 * <h3>How the three subsystems fit together</h3>
 * <pre>
 *   BaristaQueue          — live, in-memory FIFO matching (who's free now)
 *   ChatSessionRepository — durable record of sessions (survives restart)
 *   ChatRepository         — durable record of messages, scoped by session
 * </pre>
 * Every time {@link BaristaQueue} changes live state (a match happens, a
 * session ends), ChatService immediately persists that change via the
 * repositories — so the database is always a faithful snapshot of the
 * live queue, not just a log of messages.
 *
 * <h3>Thread safety</h3>
 * Observers use {@link CopyOnWriteArraySet} (same approach as
 * {@link dev.saberlabs.observer.OrderNotificationService}). The customer
 * cache is synchronized since multiple sessions could resolve concurrently.
 * {@link BaristaQueue} itself is internally lock-protected.
 */
public class ChatService {

    private static final String ORDER_COMMAND = "order";

    @NotNull private final ChatRepository chatRepository;
    @NotNull private final ChatSessionRepository sessionRepository;
    @NotNull private final BaristaQueue baristaQueue;
    @NotNull private final CoffeeShop coffeeShop;

    @NotNull private final CopyOnWriteArraySet<ChatObserver> observers =
            new CopyOnWriteArraySet<>();

    /** Maps a User's database ID to their domain Customer object. */
    @NotNull private final Map<Long, Customer> customerCache = new HashMap<>();

    @NotNull private final Map<String, CoffeeCreator> menu = Map.of(
            "espresso", new EspressoCreator(),
            "cappuccino", new CappuccinoCreator(),
            "latte", new LatteCreator()
    );

    public ChatService(@NotNull ChatRepository chatRepository,
                       @NotNull ChatSessionRepository sessionRepository,
                       @NotNull BaristaQueue baristaQueue,
                       @NotNull CoffeeShop coffeeShop) {
        this.chatRepository = Objects.requireNonNull(chatRepository, "ChatRepository cannot be null");
        this.sessionRepository = Objects.requireNonNull(sessionRepository, "ChatSessionRepository cannot be null");
        this.baristaQueue = Objects.requireNonNull(baristaQueue, "BaristaQueue cannot be null");
        this.coffeeShop = Objects.requireNonNull(coffeeShop, "CoffeeShop cannot be null");
    }

    // ================================================================
    // Customer <-> User linking
    // ================================================================

    /**
     * Resolves the domain {@link Customer} for an authenticated CUSTOMER user.
     * Creates one on first call, reuses the cached instance afterward,
     * and registers it as an Observer with the CoffeeShop singleton.
     *
     * @param user the authenticated user, must have role CUSTOMER
     * @return the linked Customer domain object
     */
    public synchronized @NotNull Customer resolveCustomer(@NotNull User user) {
        Objects.requireNonNull(user, "User cannot be null");
        if (!user.isCustomer()) {
            throw new IllegalArgumentException(
                    "Only CUSTOMER users have a linked Customer profile: " + user.username());
        }
        return customerCache.computeIfAbsent(user.id(), id -> {
            Customer customer = new Customer("CUST-" + id, user.username());
            coffeeShop.registerObserver(customer);
            return customer;
        });
    }

    // ================================================================
    // Session lifecycle — "Start Chat" entry point for customers
    // ================================================================

    /**
     * Starts or resumes a chat session for a customer.
     * *
     * If the customer has an existing non-INACTIVE session, it is resumed
     * as-is (its current WAITING or ACTIVE status and barista assignment
     * are preserved — this does NOT re-run matching). Otherwise, a new
     * session is created and immediately offered to the BaristaQueue,
     * which may match it to a READY barista right away.
     *
     * @param customerUser the customer starting the chat
     * @return the session the customer is now in (WAITING or ACTIVE)
     */
    public @NotNull ChatSession startChat(@NotNull User customerUser) {
        Objects.requireNonNull(customerUser, "Customer user cannot be null");
        if (!customerUser.isCustomer()) {
            throw new IllegalArgumentException("Only CUSTOMER users can start a chat");
        }

        Optional<ChatSession> existing =
                sessionRepository.findActiveSessionByCustomer(customerUser.id());
        if (existing.isPresent()) {
            return existing.get();
        }

        ChatSession newSession = ChatSession.newWaitingSession(customerUser.id());
        ChatSession persisted = sessionRepository.save(newSession);

        ChatSession afterMatching = baristaQueue.customerWaiting(persisted);
        if (afterMatching.isActive()) {
            sessionRepository.save(afterMatching);
            notifySessionMatched(afterMatching);
        }
        return afterMatching;
    }

    /**
     * Marks a barista as READY to take the next available session.
     * If a customer is already WAITING, the barista is matched immediately.
     *
     * @param baristaUser the barista becoming available
     * @return the session the barista was matched with, if any
     */
    public @NotNull Optional<ChatSession> baristaReady(@NotNull User baristaUser) {
        Objects.requireNonNull(baristaUser, "Barista user cannot be null");
        if (!baristaUser.isBarista()) {
            throw new IllegalArgumentException("Only BARISTA users can go READY");
        }

        Optional<ChatSession> matched = baristaQueue.baristaReady(baristaUser.id());
        matched.ifPresent(session -> {
            sessionRepository.save(session);
            notifySessionMatched(session);
        });
        return matched;
    }

    /**
     * Removes a barista from the READY pool (e.g. logging out).
     * Does not affect sessions they are already ACTIVE on.
     *
     * @param baristaUser the barista going OFFLINE
     */
    public void baristaOffline(@NotNull User baristaUser) {
        Objects.requireNonNull(baristaUser, "Barista user cannot be null");
        baristaQueue.baristaOffline(baristaUser.id());
    }

    /**
     * Ends a session — called explicitly when the customer or barista
     * decides the conversation is over (e.g. typing "end chat" or
     * "close session"). Marks it INACTIVE, persists that, and frees
     * the assigned barista, who is immediately rematched if anyone
     * is WAITING.
     * *
     * Note: this is intentionally NOT triggered automatically by order
     * fulfillment — a customer may want to keep chatting after their
     * coffee is ready (additional orders, questions, complaints).
     *
     * @param sessionId the session to end
     * @return the barista's next session if they were rematched, or empty
     */
    public @NotNull Optional<ChatSession> endSession(long sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session ->
                sessionRepository.save(session.deactivate()));

        Optional<ChatSession> rematch = baristaQueue.sessionEnded(sessionId);
        rematch.ifPresent(session -> {
            sessionRepository.save(session);
            notifySessionMatched(session);
        });
        return rematch;
    }

    /**
     * Returns every session in the system — used by the barista dashboard
     * to show all sessions, their status, and who is responsible for each.
     *
     * @return unmodifiable list of all sessions, newest first
     */
    public @NotNull List<ChatSession> getAllSessions() {
        return sessionRepository.findAll();
    }

    /**
     * Returns a barista's currently ACTIVE sessions — used when a barista
     * logs back in and needs to resume conversations they were handling.
     *
     * @param baristaUser the barista
     * @return unmodifiable list of that barista's active sessions
     */
    public @NotNull List<ChatSession> getActiveSessionsForBarista(@NotNull User baristaUser) {
        Objects.requireNonNull(baristaUser, "Barista user cannot be null");
        return sessionRepository.findActiveSessionsByBarista(baristaUser.id());
    }


    /**
     * Re-synchronizes the in-memory {@link BaristaQueue} with whatever
     * sessions were left in the database from a previous run.
     * *
     * Must be called once at application startup, before any barista calls
     * {@link #baristaReady(User)} and before any customer calls
     * {@link #startChat(User)} — otherwise, an ACTIVE session from a prior
     * run would be invisible to the queue, and a barista who was mid-conversation
     * before a restart could be handed a second customer on top of the one
     * they're still actually talking to
     *
     * <h3>What this does</h3>
     * <ul>
     *   <li>ACTIVE sessions — the assigned barista is marked busy by simply
     *       NOT adding them to the ready pool. No queue action needed; the
     *       absence from the ready pool IS the "busy" state.</li>
     *   <li>WAITING sessions — re-enqueued via {@link BaristaQueue#customerWaiting}
     *       so they're immediately matchable once a barista goes READY.</li>
     *   <li>INACTIVE sessions — ignored, already historical.</li>
     * </ul>
     */
    public void recoverSessionsOnStartup() {
        List<ChatSession> all = sessionRepository.findAll();

        long waitingRecovered = all.stream()
                .filter(ChatSession::isWaiting)
                .peek(baristaQueue::customerWaiting)
                .count();

        long activeRecovered = all.stream()
                .filter(ChatSession::isActive)
                .count();

        System.out.printf(
                "[ChatService] Startup recovery: %d WAITING session(s) re-queued, " +
                        "%d ACTIVE session(s) acknowledged (their baristas remain busy " +
                        "until they go READY again or the session ends).%n",
                waitingRecovered, activeRecovered);
    }

    // ================================================================
    // Sending and processing messages
    // ================================================================

    /**
     * Sends a plain chat message within a session — saves it and notifies observers.
     *
     * @param sessionId  the session this message belongs to
     * @param senderId   the sender's user ID
     * @param senderName the sender's display name
     * @param content    the message text
     * @return the persisted message
     */
    public @NotNull ChatMessage sendMessage(long sessionId, long senderId,
                                            @NotNull String senderName,
                                            @NotNull String content) {
        return sendMessage(sessionId, senderId, senderName, content, null);
    }

    /**
     * Sends a chat message associated with an order — saves it and notifies observers.
     */
    public @NotNull ChatMessage sendMessage(long sessionId, long senderId,
                                            @NotNull String senderName,
                                            @NotNull String content,
                                            @Nullable String orderId) {
        Objects.requireNonNull(senderName, "Sender name cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");

        ChatMessage message = ChatMessage.of(sessionId, senderId, senderName, content, orderId);
        ChatMessage saved = chatRepository.save(message);
        notifyObservers(saved);
        return saved;
    }



    /**
     * Processes raw input from a customer inside an active session — either
     * a plain chat message or an "order &lt;coffee&gt; [extras]" command.
     *
     * @param user    the customer sending the input
     * @param session the session this input belongs to
     * @param input   the raw text typed by the customer
     * @return the resulting chat message
     */
    public @NotNull ChatMessage processCustomerInput(@NotNull User user,
                                                     @NotNull ChatSession session,
                                                     @NotNull String input) {
        Objects.requireNonNull(user, "User cannot be null");
        Objects.requireNonNull(session, "Session cannot be null");
        Objects.requireNonNull(input, "Input cannot be null");

        String trimmed = input.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith(ORDER_COMMAND)) {
            return handleOrderCommand(user, session, trimmed);
        }
        return sendMessage(session.id(), user.id(), user.username(), trimmed);
    }

    // ================================================================
    // Order command parsing
    // ================================================================

    private @NotNull ChatMessage handleOrderCommand(@NotNull User user,
                                                    @NotNull ChatSession session,
                                                    @NotNull String input) {
        sendMessage(session.id(), user.id(), user.username(), input);

        String[] parts = input.trim().split("\\s+");
        if (parts.length < 2) {
            return sendSystemMessage(session.id(),
                    "Please specify a coffee type. Example: order espresso milk");
        }

        String coffeeType = parts[1].toLowerCase(Locale.ROOT);
        CoffeeCreator creator = menu.get(coffeeType);
        if (creator == null) {
            return sendSystemMessage(session.id(),
                    "Unknown coffee type: " + coffeeType
                            + ". Available: " + String.join(", ", menu.keySet()));
        }

        Coffee coffee = creator.createCoffee();

        List<String> unknownExtras = new ArrayList<>();
        for (int i = 2; i < parts.length; i++) {
            String extra = parts[i].toLowerCase(Locale.ROOT);
            Coffee decorated = applyExtra(coffee, extra);
            if (decorated == coffee) {
                unknownExtras.add(extra);
            } else {
                coffee = decorated;
            }
        }

        if (!unknownExtras.isEmpty()) {
            return sendSystemMessage(session.id(),
                    "Unknown extra(s): " + String.join(", ", unknownExtras)
                            + ". Available: milk, sugar, whipped");
        }

        Customer customer = resolveCustomer(user);
        Order order = new Order(customer, coffee, coffeeShop.nextOrderId());
        coffeeShop.placeOrder(order);

        String confirmation = String.format(
                "Order placed! %s — $%.2f (Order #%s). Waiting for the barista to send it to the kitchen.",
                coffee.getDescription(), order.getFinalPrice(), order.getOrderId());

        return sendMessage(session.id(), 0, "System", confirmation, order.getOrderId());
    }

    /**
     * Called by a human barista to manually push an order from a chat
     * session into the existing automated pipeline — Factory/Decorator
     * already happened when the order was created; this step enqueues
     * it for the worker Barista threads to actually prepare.
     *
     * The barista is the "waiter" — they decide when to send it to the
     * kitchen. The worker threads (Template Method) are unaffected and
     * remain fully automated, exactly as in the multithreading project.
     *
     * @param session the session containing the order
     * @param orderId the order to send to the kitchen
     * @throws InterruptedException if the worker queue is full and blocks
     */
    public void sendOrderToKitchen(@NotNull ChatSession session,
                                   @NotNull String orderId) throws InterruptedException {
        Objects.requireNonNull(session, "Session cannot be null");
        Objects.requireNonNull(orderId, "Order ID cannot be null");

        var queue = coffeeShop.getOrderQueue();
        if (queue == null) {
            sendSystemMessage(session.id(), "The shop is not open right now.");
            return;
        }

        coffeeShop.getOrders().stream()
                .filter(o -> o.getOrderId().equals(orderId))
                .findFirst()
                .ifPresentOrElse(
                        order -> {
                            try {
                                queue.enqueue(order);
                                sendSystemMessage(session.id(),
                                        "Order #" + orderId + " sent to the kitchen!");
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        },
                        () -> sendSystemMessage(session.id(),
                                "Order #" + orderId + " not found.")
                );
    }

    private @NotNull Coffee applyExtra(@NotNull Coffee coffee, @NotNull String extra) {
        return switch (extra) {
            case "milk" -> new MilkDecorator(coffee);
            case "sugar" -> new SugarDecorator(coffee);
            case "whipped", "whippedcream", "whipped_cream" -> new WhippedCreamDecorator(coffee);
            default -> coffee;
        };
    }

    private @NotNull ChatMessage sendSystemMessage(long sessionId, @NotNull String content) {
        return sendMessage(sessionId, 0, "System", content);
    }

    // ================================================================
    // History
    // ================================================================

    /**
     * Loads the conversation history for a single session, oldest first.
     *
     * @param sessionId the session to load
     * @return that session's full message history
     */
    public @NotNull List<ChatMessage> loadHistory(long sessionId) {
        return chatRepository.findBySessionId(sessionId);
    }

    // ================================================================
    // Observer management and notification
    // ================================================================

    public void registerObserver(@NotNull ChatObserver observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");
        observers.add(observer);
    }

    public void removeObserver(@NotNull ChatObserver observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");
        observers.remove(observer);
    }

    private void notifyObservers(@NotNull ChatMessage message) {
        for (ChatObserver observer : observers) {
            observer.onMessageReceived(message);
        }
    }

    /**
     * Sends a system notification into a newly matched session so both
     * parties see confirmation immediately, before any human types anything.
     */
    private void notifySessionMatched(@NotNull ChatSession session) {
        sendSystemMessage(session.id(),
                "You are now connected. Barista ID: " + session.baristaId());
    }

    /**
     * Returns this customer's orders from the existing CoffeeShop domain,
     * filtered to just their own. Used by CustomerView's "My Order History".
     *
     * @param customer the domain Customer to filter by
     * @return that customer's orders
     */
    public @NotNull List<Order> getCoffeeShopOrdersFor(@NotNull Customer customer) {
        Objects.requireNonNull(customer, "Customer cannot be null");
        return coffeeShop.getOrders().stream()
                .filter(o -> o.getCustomer().equals(customer))
                .toList();
    }

    // Add to ChatService

    /**
     * Returns the customer's orders that are still PLACED — i.e. created
     * via chat but not yet sent to the kitchen by a barista. Used by
     * BaristaView to show a reviewable menu instead of requiring the
     * barista to recall or copy-paste an exact order ID from the transcript.
     *
     * @param session the session whose customer's orders should be checked
     * @return that customer's PLACED orders, oldest first
     */
    public @NotNull List<Order> getPendingOrdersForSession(@NotNull ChatSession session) {
        Objects.requireNonNull(session, "Session cannot be null");
        return coffeeShop.getOrders().stream()
                .filter(o -> o.getCustomer().getId().equals("CUST-" + session.customerId()))
                .filter(o -> o.getStatus() == OrderStatus.PLACED)
                .toList();
    }
}
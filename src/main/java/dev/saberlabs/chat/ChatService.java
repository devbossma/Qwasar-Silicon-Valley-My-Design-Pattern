package dev.saberlabs.chat;

import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.auth.User;
import dev.saberlabs.chat.repositories.ChatOrderRepository;
import dev.saberlabs.chat.repositories.ChatRepository;
import dev.saberlabs.chat.repositories.ChatSessionRepository;
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
import dev.saberlabs.order.StoredOrder;
import dev.saberlabs.singleton.CoffeeShop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Coordinates the full chat experience: barista-pool session matching,
 * message persistence, order placement via chat commands, and Observer
 * notifications to console/UI listeners.
 * *
 * Order ID continuity is owned by {@link ChatOrderRepository#nextOrderId()},
 * which queries the actual orders table rather than trusting an in-memory
 * counter — this is what survives application restarts without ID collisions.
 */
public class ChatService {

    private static final String ORDER_COMMAND = "order";

    @NotNull private final ChatRepository chatRepository;
    @NotNull private final ChatSessionRepository sessionRepository;
    @NotNull private final ChatOrderRepository orderRepository;
    @NotNull private final ChatNotificationService notificationService;
    @NotNull private final BaristaQueue baristaQueue;
    @NotNull private final CoffeeShop coffeeShop;

    @NotNull private final CopyOnWriteArraySet<ChatObserver> observers =
            new CopyOnWriteArraySet<>();

    @NotNull private final Map<Long, Customer> customerCache = new HashMap<>();

    @NotNull private final Map<String, CoffeeCreator> menu = Map.of(
            "espresso", new EspressoCreator(),
            "cappuccino", new CappuccinoCreator(),
            "latte", new LatteCreator()
    );

    public ChatService(@NotNull ChatRepository chatRepository,
                       @NotNull ChatSessionRepository sessionRepository,
                       @NotNull ChatOrderRepository orderRepository,
                       @NotNull ChatNotificationService notificationService,
                       @NotNull BaristaQueue baristaQueue,
                       @NotNull CoffeeShop coffeeShop) {
        this.chatRepository      = Objects.requireNonNull(chatRepository);
        this.sessionRepository   = Objects.requireNonNull(sessionRepository);
        this.orderRepository     = Objects.requireNonNull(orderRepository);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.baristaQueue        = Objects.requireNonNull(baristaQueue);
        this.coffeeShop          = Objects.requireNonNull(coffeeShop);
    }

    // ================================================================
    // Customer <-> User linking
    // ================================================================

    public synchronized @NotNull Customer resolveCustomer(@NotNull User user) {
        Objects.requireNonNull(user, "User cannot be null");
        if (!user.isCustomer()) {
            throw new IllegalArgumentException(
                    "Only CUSTOMER users have a linked Customer profile: " + user.username());
        }
        return customerCache.computeIfAbsent(user.id(), id -> {
            Customer customer = new Customer("CUST-" + id, user.username());

            int fulfilledCount = (int) orderRepository.findByCustomer(id).stream()
                    .filter(o -> o.status().equals(OrderStatus.FULFILLED.name()))
                    .count();

            customer.restoreTotalOrders(fulfilledCount);

            coffeeShop.registerObserver(customer);
            return customer;
        });
    }

    // ================================================================
    // Session lifecycle
    // ================================================================

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

    public void baristaOffline(@NotNull User baristaUser) {
        Objects.requireNonNull(baristaUser, "Barista user cannot be null");
        baristaQueue.baristaOffline(baristaUser.id());
    }

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

    public @NotNull List<ChatSession> getAllSessions() {
        return sessionRepository.findAll();
    }

    public @NotNull List<ChatSession> getActiveSessionsForBarista(@NotNull User baristaUser) {
        Objects.requireNonNull(baristaUser, "Barista user cannot be null");
        return sessionRepository.findActiveSessionsByBarista(baristaUser.id());
    }

    /**
     * Re-synchronizes the in-memory {@link BaristaQueue} with sessions
     * left in the database from a previous run. Must be called once at
     * startup, before any baristaReady()/startChat() calls.
     */
    public void recoverSessionsOnStartup() {
        List<ChatSession> all = sessionRepository.findAll();

        long waitingRecovered = all.stream()
                .filter(ChatSession::isWaiting)
                .peek(baristaQueue::customerWaiting)
                .count();

        long activeRecovered = 0;
        for (ChatSession session : all) {
            if (session.isActive() && session.baristaId() != null) {
                baristaQueue.restoreActiveAssignment(session.id(), session.baristaId());
                activeRecovered++;
            }
        }

        System.out.printf(
                "[ChatService] Startup recovery: %d WAITING session(s) re-queued, " +
                        "%d ACTIVE session(s) restored (their baristas remain busy " +
                        "until they go READY again or the session ends).%n",
                waitingRecovered, activeRecovered);
    }

    // ================================================================
    // Sending and processing messages
    // ================================================================

    public @NotNull ChatMessage sendMessage(long sessionId, long senderId,
                                            @NotNull String senderName,
                                            @NotNull String content,
                                            @NotNull MessageType type,
                                            @Nullable String orderId) {
        Objects.requireNonNull(senderName, "Sender name cannot be null");
        Objects.requireNonNull(content, "Content cannot be null");
        Objects.requireNonNull(type, "Message type cannot be null");

        ChatMessage message = ChatMessage.of(type, sessionId, senderId, senderName,
                content,  orderId);
        ChatMessage saved = chatRepository.save(message);
        notifyObservers(saved);
        return saved;
    }

    public @NotNull ChatMessage sendMessage(long sessionId, long senderId,
                                            @NotNull String senderName,
                                            @NotNull String content) {
        return sendMessage(sessionId, senderId, senderName, content,
                MessageType.CHAT_MESSAGE, null);
    }

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
    // Order command parsing — now persists a StoredOrder row
    // ================================================================

    private @NotNull ChatMessage handleOrderCommand(@NotNull User user,
                                                    @NotNull ChatSession session,
                                                    @NotNull String input) {
        // Customer's own typed command — CHAT_MESSAGE
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

        List<String> appliedExtras = new ArrayList<>();
        List<String> unknownExtras = new ArrayList<>();
        for (int i = 2; i < parts.length; i++) {
            String extra = parts[i].toLowerCase(Locale.ROOT);
            Coffee decorated = applyExtra(coffee, extra);
            if (decorated == coffee) {
                unknownExtras.add(extra);
            } else {
                coffee = decorated;
                appliedExtras.add(extra);
            }
        }

        if (!unknownExtras.isEmpty()) {
            return sendSystemMessage(session.id(),
                    "Unknown extra(s): " + String.join(", ", unknownExtras)
                            + ". Available: milk, sugar, whipped");
        }

        Customer customer = resolveCustomer(user);
        String orderId = orderRepository.nextOrderId();
        Order order = new Order(customer, coffee, orderId);
        coffeeShop.placeOrder(order);

        StoredOrder stored = new StoredOrder(
                orderId, user.id(), null, session.id(),
                coffeeType, appliedExtras, order.getFinalPrice(),
                OrderStatus.PLACED.name(), LocalDateTime.now()
        );
        orderRepository.save(stored);

        String confirmation = String.format(
                "Order placed! %s — $%.2f (Order #%s). Waiting for the barista to send it to the kitchen.",
                coffee.getDescription(), order.getFinalPrice(), order.getOrderId());

        // Confirmation is a SYSTEM_MESSAGE carrying the order's ID
        return sendSystemMessage(session.id(), confirmation, order.getOrderId());
    }

    /**
     * Returns the customer's orders still PLACED — created via chat but
     * not yet sent to the kitchen. Used by BaristaView to show a
     * reviewable numbered menu instead of requiring an exact ID.
     *
     * @param session the session whose customer's orders should be checked
     * @return that customer's PLACED orders, oldest first
     */
    public @NotNull List<StoredOrder> getPendingOrdersForSession(@NotNull ChatSession session) {
        Objects.requireNonNull(session, "Session cannot be null");
        return orderRepository.findByCustomerAndStatus(
                session.customerId(), OrderStatus.PLACED.name());
    }

    /**
     * Called by a human barista to manually push an order from a chat
     * session into the existing automated pipeline. Updates the
     * persisted order row with the barista's ID and enqueues the
     * live Order domain object for the worker Barista threads.
     *
     * @param session   the session containing the order
     * @param baristaId the barista sending the order
     * @param orderId   the order to send to the kitchen
     * @throws InterruptedException if the worker queue is full and blocks
     */
    public void sendOrderToKitchen(@NotNull ChatSession session,
                                   long baristaId,
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
                                orderRepository.updateAssignmentAndStatus(
                                        orderId, baristaId, session.id(), OrderStatus.PREPARING.name());
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

    /**
     * Collects payment for a READY order and marks it FULFILLED.
     * This is the explicit human action that completes an order — the
     * worker Barista threads only ever bring an order to READY (physically
     * prepared); a chat barista must confirm payment before it's FULFILLED.
     *
     * @param session         the session containing the order
     * @param orderId         the order to collect payment for
     * @param paymentGateway  the adapter to process payment through
     *                        (PayPalAdapter, StripeAdapter, CashPaymentAdapter)
     * @return true if payment succeeded and the order is now FULFILLED
     */
    public boolean collectPaymentAndFulfill(@NotNull ChatSession session,
                                            @NotNull String orderId,
                                            @NotNull PaymentGateway paymentGateway) {
        Objects.requireNonNull(session, "Session cannot be null");
        Objects.requireNonNull(orderId, "Order ID cannot be null");
        Objects.requireNonNull(paymentGateway, "Payment gateway cannot be null");

        var orderOpt = coffeeShop.getOrders().stream()
                .filter(o -> o.getOrderId().equals(orderId))
                .findFirst();

        if (orderOpt.isEmpty()) {
            sendSystemMessage(session.id(), "Order #" + orderId + " not found.");
            return false;
        }

        Order order = orderOpt.get();
        if (order.getStatus() != OrderStatus.READY) {
            sendSystemMessage(session.id(),
                    "Order #" + orderId + " is not ready for payment yet (status: "
                            + order.getStatus() + ").");
            return false;
        }

        boolean paid = paymentGateway.processPayment(orderId, order.getFinalPrice());
        if (!paid) {
            sendSystemMessage(session.id(),
                    "Payment failed for order #" + orderId + ". Please try again.");
            return false;
        }

        // Mark fulfilled — PersistingOrderObserver will persist this transition
        order.setStatus(OrderStatus.FULFILLED);
        notificationService.notifyPaymentReceived(
                session.baristaId() != null ? session.baristaId() : 0L,
                orderId,
                order.getFinalPrice(),
                order.getCustomer().getName());
        orderRepository.updateStatus(orderId, OrderStatus.FULFILLED.name());

        // Notify both parties through the session chat
        sendSystemMessage(session.id(), String.format(
                "✅ Payment of $%.2f received for order #%s. Transaction complete!",
                order.getFinalPrice(), orderId));

        return true;
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
        return sendMessage(sessionId, 0, "System", content,
                MessageType.SYSTEM_MESSAGE, null);
    }

    private @NotNull ChatMessage sendSystemMessage(long sessionId, @NotNull String content,
                                                   @Nullable String orderId) {
        return sendMessage(sessionId, 0, "System", content,
                MessageType.SYSTEM_MESSAGE, orderId);
    }

    // ================================================================
    // Order history
    // ================================================================

    /**
     * Returns the customer's full order history from the persisted
     * orders table (not the in-memory CoffeeShop list, which resets
     * on restart). Used by CustomerView's "My Order History".
     *
     * @param user the customer
     * @return that customer's orders, newest first
     */
    public @NotNull List<StoredOrder> getOrderHistory(@NotNull User user) {
        Objects.requireNonNull(user, "User cannot be null");
        return orderRepository.findByCustomer(user.id());
    }

    /**
     * Returns all orders in the system, newest first. Used by ManagerView
     * for a global audit of all orders, regardless of customer or session.
     * @return unmodifiable list of all orders in the system
     */
    public @NotNull List<StoredOrder> getAllOrders() {
        return orderRepository.findAll();
    }

    // ================================================================
    // Chat history
    // ================================================================

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

    private void notifySessionMatched(@NotNull ChatSession session) {
        // System message in the session transcript (both parties see it)
        sendSystemMessage(session.id(),
                "You are now connected. Barista ID: " + session.baristaId());

        // User-scoped notification for the customer specifically
        notificationService.notifySessionMatched(session.customerId(), session.baristaId(), session.id());
    }

    /**
     * Returns the live Order domain objects for a customer ID, from the
     * in-memory CoffeeShop list. Used during an active session to check
     * READY status before collecting payment — only valid while the
     * order is still in memory (same JVM run it was created in).
     *
     * @param customerUserId the customer's User ID
     * @return that customer's live orders this session
     */
    public @NotNull List<Order> getCoffeeShopOrdersForCustomer(long customerUserId) {
        String customerId = "CUST-" + customerUserId;
        return coffeeShop.getOrders().stream()
                .filter(o -> o.getCustomer().getId().equals(customerId))
                .toList();
    }
}
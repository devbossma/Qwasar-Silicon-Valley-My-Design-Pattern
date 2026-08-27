package dev.saberlabs.facade;

import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.ChatService;
import dev.saberlabs.command.OrderInvoker;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.framework.business.BusinessObject;
import dev.saberlabs.framework.business.ChatDetails;
import dev.saberlabs.framework.business.FeedbackDetails;
import dev.saberlabs.framework.business.OrderDetails;
import dev.saberlabs.framework.business.RequestType;
import dev.saberlabs.framework.business.annotation.ChatHandler;
import dev.saberlabs.framework.business.annotation.OrderHandler;
import dev.saberlabs.models.*;
import dev.saberlabs.observer.OrderObserver;
import dev.saberlabs.order.OrderService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Pattern 10: FACADE

 * Provides a simplified, unified interface to the complex subsystems
 * of the coffee shop application. Without the Facade, client code would
 * need to coordinate all of these manually:
 *
 * <ul>
 *   <li><b>Singleton</b> — CoffeeShop instance for order registration</li>
 *   <li><b>Factory Method</b> — CoffeeCreator to create coffee products</li>
 *   <li><b>Decorator</b> — Milk, Sugar, WhippedCream extras</li>
 *   <li><b>Prototype</b> — Order cloning for reorders</li>
 *   <li><b>Template Method</b> — Coffee preparation steps</li>
 *   <li><b>Strategy</b> — Loyalty-based pricing (auto-resolved from Customer)</li>
 *   <li><b>Observer</b> — Order status notifications</li>
 *   <li><b>Command</b> — Order lifecycle commands with undo support</li>
 *   <li><b>Adapter</b> — Payment processing through PaymentGateway</li>
 * </ul>
 *
 * All of the above actually lives in {@link OrderService} — this class doesn't implement any
 * order logic itself, it delegates every order-related call to a single {@code OrderService}
 * instance. That keeps this Facade doing exactly what a Facade is for (a simple, unified
 * entry point), while the real application/service logic lives in one reusable place.
 *
 * <h3>The reflection framework's real BusinessObject</h3>
 * This class also composes a {@link ChatService} and implements
 * {@link dev.saberlabs.framework.business.BusinessObject} — its {@link #handleOrder} and
 * {@link #handleChat} methods delegate to the exact same {@code OrderService}/{@code ChatService}
 * this class already uses for its normal API, so reflective dispatch reaches real business
 * logic, not a stand-in. Deliberately NOT wired here: the live application's own order/chat
 * traffic ({@code ChatService}'s own methods) does not route through reflection — see
 * {@code framework/doc.md} for why forcing that would only add indirection without reducing
 * anyone's work, which is not what a real framework would do.
 *
 * <h3>Usage</h3>
 * <pre>
 *     CoffeeShopFacade facade = new CoffeeShopFacade(orderService, chatService);
 *     facade.registerCustomer(customer);
 *
 *     Order order = facade.placeOrder(customer, new EspressoCreator(), "milk", "sugar");
 *     facade.processOrder(order);
 *     facade.reorder(order);
 *     facade.reorderForAnotherCustomer(order, anotherCustomer);
 * </pre>
 */
public class CoffeeShopFacade implements BusinessObject {

    private final OrderService orderService;
    private final ChatService chatService;
    private final List<String> feedbackLog = new ArrayList<>();

    /**
     * @param orderService the real order lifecycle this facade delegates its order API to
     * @param chatService  the real chat service {@link #handleChat} delegates to
     */
    public CoffeeShopFacade(@NotNull OrderService orderService, @NotNull ChatService chatService) {
        this.orderService = Objects.requireNonNull(orderService, "Order service cannot be null");
        this.chatService = Objects.requireNonNull(chatService, "Chat service cannot be null");
    }

    // ================================================================
    // Shop Lifecycle (Singleton)
    // ================================================================

    public void open(int queueCapacity, int numberOfBaristas) {
        orderService.open(queueCapacity, numberOfBaristas);
    }

    public void close() {
        orderService.close();
    }

    // ================================================================
    // Customer Management (Observer)
    // ================================================================

    public @NotNull Customer createCustomer(@NotNull String customerName) {
        Objects.requireNonNull(customerName, "Customer name cannot be null");
        String customerId = orderService.nextCustomerId();
        return new Customer(customerId, customerName);
    }

    /**
     * Registers an observer to receive order notifications. (Observer pattern)
     * *
     * We're already registering the Customer at PlaceOrderCommand, so this method is typically
     * used for manual subscription — of a Customer, or of any other {@link OrderObserver}
     * (e.g. a persistence observer).
     * @param observer the observer to register
     */
    public void registerCustomer(@NotNull OrderObserver observer) {
        Objects.requireNonNull(observer, "Customer observer cannot be null");
        orderService.registerObserver(observer);
    }

    /**
     * Removes an observer from receiving order status notifications.
     * This can be used if a customer wants to opt out of notifications or if they are no longer active.
     * Note that customers are automatically removed as observers when their orders are fulfilled, so this method is typically used for manual unsubscription or cleanup.
     * @param observer the observer to remove
     */
    public void removeCustomer(@NotNull OrderObserver observer) {
        Objects.requireNonNull(observer, "Customer observer cannot be null");
        orderService.removeObserver(observer);
    }

    // ================================================================
    // Order Placement (Factory + Decorator + Strategy + Singleton)
    // ================================================================

    /**
     * Creates a coffee using the provided creator, applies extras,
     * builds an Order (auto-priced by customer's loyalty tier),
     * and places it.
     *
     * @param customer the customer placing the order
     * @param creator  the factory creator for the base coffee type
     * @param extras   optional extras: "milk", "sugar", "whipped_cream"
     * @return the placed Order
     */
    public @NotNull Order placeOrder(@NotNull Customer customer, @NotNull CoffeeCreator creator, @NotNull String... extras) {
        return orderService.placeOrder(customer, creator, extras);
    }

    /**
     * Places an order with a pre-built coffee (already decorated).
     * Useful when the client has already composed the coffee manually.
     *
     * @param customer the customer placing the order
     * @param coffee   the fully composed coffee
     * @return the placed Order
     */
    public @NotNull Order placeOrder(@NotNull Customer customer, @NotNull Coffee coffee) {
        return orderService.placeOrder(customer, coffee);
    }

    /**
     * Places an already-built order (e.g. one an owning caller assembled itself, with its own
     * order ID).
     *
     * @param order the already-built order to place
     * @return the same order, now placed
     */
    public @NotNull Order placeOrder(@NotNull Order order) {
        return orderService.placeOrder(order);
    }

    // ================================================================
    // Order Processing (Command + Template Method + Adapter)
    // ================================================================

    /**
     * Processes an order through the full lifecycle:
     * 1. Prepare — runs the Template Method for the coffee type
     * 2. Pay — processes payment through the Adapter
     * 3. Fulfill — marks complete, increments loyalty tier
     *
     * @param order the order to process
     * @throws IllegalStateException if no payment gateway has been configured via
     *                                {@link #setPaymentGateway(PaymentGateway)}
     */
    public void processOrder(@NotNull Order order) {
        orderService.processOrder(order);
    }

    // ================================================================
    // Reordering (Prototype)
    // ================================================================

    /**
     * Clones an existing order for the same customer.
     * The cloned order goes through the full lifecycle.
     *
     * @param previousOrder the order to clone
     * @return the new cloned and processed Order
     */
    public @NotNull Order reorder(@NotNull Order previousOrder) {
        return orderService.reorder(previousOrder);
    }

    /**
     * Clones an existing order for a different customer.
     * "I'll have what she's having."
     *
     * @param previousOrder the order to clone
     * @param newCustomer   the customer who wants the same coffee
     * @return the new cloned and processed Order
     */
    public @NotNull Order reorderForAnotherCustomer(@NotNull Order previousOrder, @NotNull Customer newCustomer) {
        return orderService.reorderForAnotherCustomer(previousOrder, newCustomer);
    }

    // ===============================================================
    // Payment Gateway Management (Adapter)
    // ===============================================================

    public void setPaymentGateway(@NotNull PaymentGateway paymentGateway) {
        orderService.setPaymentGateway(paymentGateway);
    }

    // ================================================================
    // Reflection Framework (BusinessObject) — see dev.saberlabs.framework.business
    // ================================================================

    /**
     * The {@link BusinessObject} default/fallback handler — invoked whenever a request has no
     * dedicated {@link OrderHandler}/{@link ChatHandler}-style annotated method, such as
     * {@link FeedbackDetails}. There's no dedicated "feedback" subsystem in this app, so
     * recording the raw text here for a human to review later is genuine behavior, not a
     * stand-in for a missing one; see {@link #getFeedbackLog()}.
     *
     * @param request the request with no dedicated handler
     */
    @Override
    public void processRequest(RequestType request) {
        Objects.requireNonNull(request, "Request cannot be null");
        if (request instanceof FeedbackDetails feedback) {
            feedbackLog.add(feedback.text());
        }
    }

    /**
     * Places the given, already-built order through the exact same {@link OrderService} every
     * other caller of this facade uses — reflective dispatch reaches real placement logic, not
     * a demo stand-in.
     *
     * @param details the order to place
     * @return the placed order
     */
    @OrderHandler
    public @NotNull Order handleOrder(@NotNull OrderDetails details) {
        Objects.requireNonNull(details, "Order details cannot be null");
        return orderService.placeOrder(details.order());
    }

    /**
     * Sends the given chat message through the real {@link ChatService} this facade composes.
     *
     * @param details the chat message to send
     * @return the saved, real {@link ChatMessage}
     */
    @ChatHandler
    public @NotNull ChatMessage handleChat(@NotNull ChatDetails details) {
        Objects.requireNonNull(details, "Chat details cannot be null");
        return chatService.sendMessage(details.sessionId(), details.senderId(),
                details.senderName(), details.content());
    }

    /**
     * Returns the request texts recorded via the {@link #processRequest(RequestType)} fallback.
     *
     * @return an unmodifiable snapshot of the feedback log
     */
    public @NotNull List<String> getFeedbackLog() {
        return List.copyOf(feedbackLog);
    }

    // ================================================================
    // Undo Support (Command)
    // ================================================================

    /**
     * Undoes the last executed command.
     */
    public void undoLastAction() {
        orderService.undoLastAction();
    }

    // ================================================================
    // Shop Queries (Singleton)
    // ================================================================

    /**
     * Returns all orders registered in the coffee shop.
     *
     * @return unmodifiable list of all orders
     */
    public @NotNull List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    /**
     * Returns the total number of orders placed.
     *
     * @return the order count
     */
    public int getOrderCount() {
        return orderService.getOrderCount();
    }

    /**
     * Returns the command invoker for inspecting command history.
     *
     * @return the order invoker
     */
    public @NotNull OrderInvoker getInvoker() {
        return orderService.getInvoker();
    }
}

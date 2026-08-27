package dev.saberlabs.order;

import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.command.FulfillOrderCommand;
import dev.saberlabs.command.OrderInvoker;
import dev.saberlabs.command.PayOrderCommand;
import dev.saberlabs.command.PlaceOrderCommand;
import dev.saberlabs.command.PrepareOrderCommand;
import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;
import dev.saberlabs.multithread.OrderQueue;
import dev.saberlabs.observer.OrderObserver;
import dev.saberlabs.singleton.CoffeeShop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application-layer door onto the full order lifecycle: placement (Factory + Decorator +
 * Command), processing (Template Method + Adapter, via Command), reordering (Prototype), and
 * the shop's live order/queue state (Singleton).
 * <p>
 * This is the only class in the application that holds a {@link CoffeeShop} reference for
 * order purposes. {@link dev.saberlabs.facade.CoffeeShopFacade} and every real caller —
 * {@code ChatService}, the CLI/FX entry points, the barista views/controllers — go through
 * this service (directly or via the Facade) instead of reaching {@code CoffeeShop} themselves,
 * so there's exactly one place that knows the shop singleton exists.
 * <p>
 * Out of scope: the Command classes ({@link PlaceOrderCommand}, {@link PrepareOrderCommand},
 * {@link FulfillOrderCommand}) and {@link Order} itself still call {@code CoffeeShop.getInstance()}
 * directly for their own bookkeeping (observer registration, ID generation, status-change
 * notification). Those are the tested GoF pattern implementations this project builds on, not
 * application wiring — rewriting them is a separate, larger effort than consolidating the
 * service-layer call sites this class replaces.
 */
public class OrderService {

    private final CoffeeShop coffeeShop;
    private final OrderInvoker invoker;
    private PaymentGateway paymentGateway;

    /**
     * Creates a new OrderService with no payment gateway configured yet. Order placement
     * doesn't need one — only {@link #processOrder(Order)} (and {@link #reorder}/
     * {@link #reorderForAnotherCustomer}, which call it) do, and they'll throw
     * {@link IllegalStateException} until {@link #setPaymentGateway(PaymentGateway)} is called.
     */
    public OrderService() {
        this.coffeeShop = CoffeeShop.getInstance();
        this.invoker = new OrderInvoker();
    }

    /**
     * Creates a new OrderService with a payment gateway already configured, ready to
     * {@link #processOrder(Order)} immediately.
     *
     * @param paymentGateway the adapter to process payment through
     */
    public OrderService(@NotNull PaymentGateway paymentGateway) {
        this();
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "Payment gateway cannot be null");
    }

    // ================================================================
    // Shop lifecycle (Singleton)
    // ================================================================

    /**
     * Opens the shop for multithreaded operation — see {@link CoffeeShop#open(int, int)}.
     */
    public void open(int queueCapacity, int numberOfBaristas) {
        coffeeShop.open(queueCapacity, numberOfBaristas);
    }

    /**
     * Closes the shop gracefully — see {@link CoffeeShop#close()}.
     */
    public void close() {
        coffeeShop.close();
    }

    /**
     * @return true if the shop is open (has an active order queue)
     */
    public boolean isOpen() {
        return coffeeShop.getOrderQueue() != null;
    }

    // ================================================================
    // Placement (Factory + Decorator + Command)
    // ================================================================

    public @NotNull Order placeOrder(@NotNull Customer customer, @NotNull CoffeeCreator creator, @NotNull String... extras) {
        Objects.requireNonNull(customer, "Customer cannot be null");
        Objects.requireNonNull(creator, "Coffee creator cannot be null");
        Objects.requireNonNull(extras, "Extras cannot be null");
        Coffee coffee = creator.createCoffee();
        if (extras.length > 0) {
            coffee = applyExtras(coffee, extras);
        }
        Order order = new Order(customer, coffee, coffeeShop.nextOrderId());
        invoker.executeCommand(new PlaceOrderCommand(order));
        return order;
    }

    public @NotNull Order placeOrder(@NotNull Customer customer, @NotNull Coffee coffee) {
        Objects.requireNonNull(customer, "Customer cannot be null");
        Objects.requireNonNull(coffee, "Coffee cannot be null");
        Order order = new Order(customer, coffee, coffeeShop.nextOrderId());
        invoker.executeCommand(new PlaceOrderCommand(order));
        return order;
    }

    /**
     * Places an already-built order (e.g. one an owning caller assembled itself, with its own
     * order ID) through the Command pattern, same as the other {@code placeOrder} overloads.
     * This is the hook callers with their own order-construction logic — such as
     * {@code ChatService}, which needs a database-continuous order ID — use to run their orders
     * through the same lifecycle as everything else, instead of talking to {@link CoffeeShop}
     * directly.
     */
    public @NotNull Order placeOrder(@NotNull Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        invoker.executeCommand(new PlaceOrderCommand(order));
        return order;
    }

    // ================================================================
    // Processing (Template Method + Adapter, via Command)
    // ================================================================

    /**
     * Processes an order through the full lifecycle: Prepare (Template Method) → Fulfill
     * (Observer + Strategy) → Pay (Adapter).
     *
     * @throws IllegalStateException if no payment gateway has been configured, either via the
     *                                {@link #OrderService(PaymentGateway)} constructor or
     *                                {@link #setPaymentGateway(PaymentGateway)}
     */
    public void processOrder(@NotNull Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        if (paymentGateway == null) {
            throw new IllegalStateException(
                    "No payment gateway configured — call setPaymentGateway(...) before processOrder().");
        }
        invoker.executeCommand(new PrepareOrderCommand(order));
        invoker.executeCommand(new FulfillOrderCommand(order));
        invoker.executeCommand(new PayOrderCommand(order, paymentGateway));
    }

    public void setPaymentGateway(@NotNull PaymentGateway paymentGateway) {
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "Payment gateway cannot be null");
    }

    // ================================================================
    // Reordering (Prototype)
    // ================================================================

    public @NotNull Order reorder(@NotNull Order previousOrder) {
        Objects.requireNonNull(previousOrder, "Previous order cannot be null");
        Order clonedOrder = previousOrder.cloneOrder();
        invoker.executeCommand(new PlaceOrderCommand(clonedOrder));
        processOrder(clonedOrder);
        return clonedOrder;
    }

    public @NotNull Order reorderForAnotherCustomer(@NotNull Order previousOrder, @NotNull Customer newCustomer) {
        Objects.requireNonNull(previousOrder, "Previous order cannot be null");
        Objects.requireNonNull(newCustomer, "New customer cannot be null");
        Order clonedOrder = previousOrder.cloneOrder(newCustomer);
        invoker.executeCommand(new PlaceOrderCommand(clonedOrder));
        processOrder(clonedOrder);
        return clonedOrder;
    }

    // ================================================================
    // Undo (Command)
    // ================================================================

    public void undoLastAction() {
        invoker.undoLastCommand();
    }

    public @NotNull OrderInvoker getInvoker() {
        return invoker;
    }

    // ================================================================
    // Notifications (Observer) — order-status subscription. Customer
    // *identity* creation stays with CoffeeShopFacade.
    // ================================================================

    public void registerObserver(@NotNull OrderObserver observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");
        coffeeShop.registerObserver(observer);
    }

    public void removeObserver(@NotNull OrderObserver observer) {
        Objects.requireNonNull(observer, "Observer cannot be null");
        coffeeShop.removeObserver(observer);
    }

    // ================================================================
    // Identifiers — customer IDs share the same shop-wide counters
    // CoffeeShop already owns for order IDs; exposed here so nothing
    // outside this service still needs a raw CoffeeShop reference.
    // ================================================================

    public @NotNull String nextCustomerId() {
        return coffeeShop.nextCustomerId();
    }

    // ================================================================
    // Queries — live order/queue state
    // ================================================================

    public @NotNull List<Order> getAllOrders() {
        return coffeeShop.getOrders();
    }

    public int getOrderCount() {
        return coffeeShop.getOrderCount();
    }

    public @NotNull Optional<Order> findByOrderId(@NotNull String orderId) {
        Objects.requireNonNull(orderId, "Order id cannot be null");
        return coffeeShop.getOrders().stream()
                .filter(o -> o.getOrderId().equals(orderId))
                .findFirst();
    }

    public @NotNull List<Order> findByCustomerId(@NotNull String customerId) {
        Objects.requireNonNull(customerId, "Customer id cannot be null");
        return coffeeShop.getOrders().stream()
                .filter(o -> o.getCustomer().getId().equals(customerId))
                .toList();
    }

    /**
     * A read-only snapshot of the kitchen queue's fill level, for display purposes
     * (e.g. a barista dashboard) — deliberately not the live {@link OrderQueue} itself,
     * since callers outside this service have no business calling {@code enqueue}/{@code dequeue}
     * on it directly.
     */
    public record QueueSnapshot(int size, int capacity) {
    }

    /**
     * @return a snapshot of the kitchen queue, or {@code null} if the shop is not open
     */
    public @Nullable QueueSnapshot getQueueSnapshot() {
        OrderQueue queue = coffeeShop.getOrderQueue();
        return queue == null ? null : new QueueSnapshot(queue.size(), queue.getCapacity());
    }

    // ================================================================
    // Kitchen handoff (Producer-Consumer)
    // ================================================================

    /**
     * Enqueues an already-placed order into the shop's worker-thread pipeline.
     *
     * @throws IllegalStateException if the shop is not open
     * @throws InterruptedException  if the queue is full and the thread is interrupted while waiting
     */
    public void sendToKitchen(@NotNull Order order) throws InterruptedException {
        Objects.requireNonNull(order, "Order cannot be null");
        OrderQueue queue = coffeeShop.getOrderQueue();
        if (queue == null) {
            throw new IllegalStateException("Shop is not open. Call open() before sendToKitchen().");
        }
        queue.enqueue(order);
    }

    // ================================================================
    // Internal helpers
    // ================================================================

    private @NotNull Coffee applyExtras(@NotNull Coffee coffee, @NotNull String... extras) {
        for (String extra : extras) {
            Objects.requireNonNull(extra, "Extra cannot be null");
            coffee = switch (extra.toLowerCase()) {
                case "milk" -> new MilkDecorator(coffee);
                case "sugar" -> new SugarDecorator(coffee);
                case "whipped_cream" -> new WhippedCreamDecorator(coffee);
                default -> throw new IllegalArgumentException("Unknown extra: " + extra);
            };
        }
        return coffee;
    }
}

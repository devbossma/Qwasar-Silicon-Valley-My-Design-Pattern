package dev.saberlabs.facade;

import dev.saberlabs.adapter.*;
import dev.saberlabs.command.FulfillOrderCommand;
import dev.saberlabs.command.OrderInvoker;
import dev.saberlabs.command.PayOrderCommand;
import dev.saberlabs.command.PlaceOrderCommand;
import dev.saberlabs.command.PrepareOrderCommand;
import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.factory.LatteCreator;
import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.framework.BusinessObject;
import dev.saberlabs.framework.ChatHandler;
import dev.saberlabs.framework.OrderHandler;
import dev.saberlabs.models.*;
import dev.saberlabs.observer.OrderObserver;
import dev.saberlabs.singleton.CoffeeShop;
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
 * The Facade reduces all of this to a few simple method calls.
 *
 * <h3>Usage</h3>
 * <pre>
 *     CoffeeShopFacade facade = new CoffeeShopFacade(paymentGateway);
 *     facade.registerCustomer(customer);
 *
 *     Order order = facade.placeOrder(customer, new EspressoCreator(), "milk", "sugar");
 *     facade.processOrder(order);
 *     facade.reorder(order);
 *     facade.reorderForAnotherCustomer(order, anotherCustomer);
 * </pre>
 */
public class CoffeeShopFacade implements BusinessObject {

    private final CoffeeShop coffeeShop;
    private final OrderInvoker invoker;
    private PaymentGateway paymentGateway;
    private final List<String> chatLog = new ArrayList<>();
    private Customer walkInCustomer;

    /**
     * Creates a new CoffeeShopFacade.
        * Initializes the CoffeeShop singleton, the Command invoker, and sets a default payment gateway adapter.
     */
    public CoffeeShopFacade(@NotNull PaymentGateway paymentGateway) {
        this.coffeeShop = CoffeeShop.getInstance();
        this.invoker = new OrderInvoker();
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "Payment gateway cannot be null");
    }

    // ================================================================
    // Customer Management (Observer)
    // ================================================================

    public @NotNull Customer createCustomer(@NotNull String customerName) {
        Objects.requireNonNull(customerName, "Customer name cannot be null");
        // get a random unique ID for the customer (for simplicity, using the nextCustomerId from CoffeeShop)
        String customerId = coffeeShop.nextCustomerId();
        return new Customer(customerId, customerName);
    }

    /**
     * Registers a customer as an observer to receive order notifications. ( Observer pattern)
     * *
     * We're already registering the Customer at PlaceOrderCommand. so this method is typically used for manual subscription.
     * @param customer the customer to register
     */
    public void registerCustomer(@NotNull OrderObserver customer) {
        Objects.requireNonNull(customer, "Customer observer cannot be null");
        coffeeShop.registerObserver(customer);
    }

    /**
     * Removes a customer from receiving order notifications.
     * This can be used if a customer wants to opt out of notifications or if they are no longer active.
     * Note that customers are automatically removed as observers when their orders are fulfilled, so this method is typically used for manual unsubscription or cleanup.
     * @param customer the customer to remove
     */
    public void removeCustomer(@NotNull OrderObserver customer) {
        Objects.requireNonNull(customer, "Customer observer cannot be null");
        coffeeShop.removeObserver(customer);
    }

    // ================================================================
    // Order Placement (Factory + Decorator + Strategy + Singleton)
    // ================================================================

    /**
     * Creates a coffee using the provided creator, applies extras,
     * builds an Order (auto-priced by customer's loyalty tier),
     * and registers it with the CoffeeShop singleton.
     *
     * @param customer the customer placing the order
     * @param creator  the factory creator for the base coffee type
     * @param extras   optional extras: "milk", "sugar", "whipped_cream"
     * @return the placed Order
     */
    public @NotNull Order placeOrder(@NotNull Customer customer, @NotNull CoffeeCreator creator, @NotNull String... extras) {
        Objects.requireNonNull(customer, "Customer cannot be null");
        Objects.requireNonNull(creator, "Coffee creator cannot be null");
        Objects.requireNonNull(extras, "Extras cannot be null");
        // Factory Method — create the base coffee
        Coffee coffee = creator.createCoffee();

        // Decorator — apply extras
        if(extras.length > 0) {
            coffee = applyExtras(coffee, extras);
        }


        // Strategy — pricing auto-resolved from customer's loyalty tier
        Order order = new Order(customer, coffee, coffeeShop.nextOrderId());

        // Command + Singleton — place the order
        invoker.executeCommand(new PlaceOrderCommand(order));

        return order;
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
        Objects.requireNonNull(customer, "Customer cannot be null");
        Objects.requireNonNull(coffee, "Coffee cannot be null");
        Order order = new Order(customer, coffee,  coffeeShop.nextOrderId());
        invoker.executeCommand(new PlaceOrderCommand(order));
        return order;
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
     */
    public void processOrder(@NotNull Order order) {
        Objects.requireNonNull(order, "Order cannot be null");
        // Template Method — prepare the coffee
        invoker.executeCommand(new PrepareOrderCommand(order));

        // Observer + Strategy — fulfill and increment loyalty
        invoker.executeCommand(new FulfillOrderCommand(order));

        // Adapter — collect payment
        invoker.executeCommand(new PayOrderCommand(order, paymentGateway));
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
        Objects.requireNonNull(previousOrder, "Previous order cannot be null");
        Order clonedOrder = previousOrder.cloneOrder();
        invoker.executeCommand(new PlaceOrderCommand(clonedOrder));
        processOrder(clonedOrder);
        return clonedOrder;
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
        Objects.requireNonNull(previousOrder, "Previous order cannot be null");
        Objects.requireNonNull(newCustomer, "New customer cannot be null");
        Order clonedOrder = previousOrder.cloneOrder(newCustomer);
        invoker.executeCommand(new PlaceOrderCommand(clonedOrder));
        processOrder(clonedOrder);
        return clonedOrder;
    }

    // ===============================================================
    // Payment Gateway Management (Adapter)
    // ===============================================================

    public void setPaymentGateway(@NotNull PaymentGateway paymentGateway) {
        this.paymentGateway = Objects.requireNonNull(paymentGateway, "Payment gateway cannot be null");
    }

    // ================================================================
    // Reflection Framework (BusinessObject) — see dev.saberlabs.framework
    // ================================================================

    /**
     * Satisfies the {@link BusinessObject} contract. Actual dispatch happens through
     * the {@link OrderHandler}/{@link ChatHandler}-annotated methods below, invoked
     * reflectively by {@code InteractionHandler} — this method itself does nothing.
     */
    @Override
    public void processRequest(String request) {
        // No-op; actual dispatch happens through annotated methods below
    }

    /**
     * Places and fully processes a real order for a walk-in customer, resolving the
     * coffee type from a case-insensitive keyword match in {@code orderDetails}
     * (defaulting to espresso). Runs the exact same lifecycle any other order goes
     * through — {@link #placeOrder(Customer, CoffeeCreator, String...)} followed by
     * {@link #processOrder(Order)} — so Command history, the Template Method
     * preparation steps, Adapter payment, and Observer/Strategy fulfillment all fire
     * the same way they would for a normal facade caller.
     *
     * @param orderDetails free-text order description, e.g. "1 Cappuccino"
     */
    @OrderHandler
    public void handleOrder(@NotNull String orderDetails) {
        if (walkInCustomer == null) {
            walkInCustomer = createCustomer("Walk-in Customer");
        }
        Order order = placeOrder(walkInCustomer, resolveCreator(orderDetails));
        processOrder(order);
    }

    /**
     * Records a chat message. This facade doesn't own the real chat subsystem
     * ({@code ChatService} does — see {@link #getChatLog()}); this is a lightweight
     * demonstration handler for the reflection framework.
     *
     * @param message the chat message
     */
    @ChatHandler
    public void handleChat(@NotNull String message) {
        chatLog.add(Objects.requireNonNull(message, "Message cannot be null"));
    }

    /**
     * Returns the messages recorded via {@link #handleChat(String)}.
     *
     * @return an unmodifiable snapshot of the chat log
     */
    public @NotNull List<String> getChatLog() {
        return List.copyOf(chatLog);
    }

    private static @NotNull CoffeeCreator resolveCreator(@NotNull String orderDetails) {
        String lower = orderDetails.toLowerCase();
        if (lower.contains("latte")) return new LatteCreator();
        if (lower.contains("cappuccino")) return new CappuccinoCreator();
        return new EspressoCreator();
    }

    // ================================================================
    // Undo Support (Command)
    // ================================================================

    /**
     * Undoes the last executed command.
     */
    public void undoLastAction() {
        invoker.undoLastCommand();
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
        return coffeeShop.getOrders();
    }

    /**
     * Returns the total number of orders placed.
     *
     * @return the order count
     */
    public int getOrderCount() {
        return coffeeShop.getOrderCount();
    }

    /**
     * Returns the command invoker for inspecting command history.
     *
     * @return the order invoker
     */
    public @NotNull OrderInvoker getInvoker() {
        return invoker;
    }

    // ================================================================
    // Internal Helpers
    // ================================================================

    /**
     * Applies decorator extras to a base coffee.
     *
     * @param coffee the base coffee
     * @param extras the extras to apply: "milk", "sugar", "whippedcream"
     * @return the decorated coffee
     */
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

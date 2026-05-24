package dev.saberlabs.facade;

import dev.saberlabs.adapter.*;
import dev.saberlabs.command.FulfillOrderCommand;
import dev.saberlabs.command.OrderInvoker;
import dev.saberlabs.command.PayOrderCommand;
import dev.saberlabs.command.PlaceOrderCommand;
import dev.saberlabs.command.PrepareOrderCommand;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.models.*;
import dev.saberlabs.observer.OrderObserver;
import dev.saberlabs.singleton.CoffeeShop;

import java.util.List;
import java.util.Scanner;

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
 *     Order order = facade.placeOrder(customer, new EspressoCreator(), "milk", "sugar");
 *     facade.processOrder(order);
 *     facade.reorder(order);
 *     facade.reorderForAnotherCustomer(order, anotherCustomer);
 * </pre>
 */
public class CoffeeShopFacade {

    private final CoffeeShop coffeeShop;
    private final OrderInvoker invoker;
    private PaymentGateway paymentGateway;

    /**
     * Creates a new CoffeeShopFacade.
        * Initializes the CoffeeShop singleton, the Command invoker, and sets a default payment gateway adapter.
     */
    public CoffeeShopFacade(PaymentGateway paymentGateway) {
        this.coffeeShop = CoffeeShop.getInstance();
        this.invoker = new OrderInvoker();
        this.paymentGateway = paymentGateway;
    }

    // ================================================================
    // Customer Management (Observer)
    // ================================================================

    public Customer createCustomer(String customerName) {
        // get a random unique ID for the customer (for simplicity, using the nextCustomerId from CoffeeShop)
        String customerId = coffeeShop.nextCustomerId();
        return new Customer(customerId, customerName);
    }

    /**
     * Registers a customer as an observer to receive order notifications. ( Observer pattern)
     *
     * We're already registering the Customer at PlaceOrderCommand. so this method is typically used for manual subscription.
     * @param customer the customer to register
     */
    public void registerCustomer(OrderObserver customer) {
        coffeeShop.registerObserver(customer);
    }

    /**
     * Removes a customer from receiving order notifications.
     * This can be used if a customer wants to opt out of notifications or if they are no longer active.
     * Note that customers are automatically removed as observers when their orders are fulfilled, so this method is typically used for manual unsubscription or cleanup.
     * @param customer the customer to remove
     */
    public void removeCustomer(OrderObserver customer) {
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
    public Order placeOrder(Customer customer, CoffeeCreator creator, String... extras) {
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
    public Order placeOrder(Customer customer, Coffee coffee) {
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
    public void processOrder(Order order) {
        // Template Method — prepare the coffee
        invoker.executeCommand(new PrepareOrderCommand(order));

        // Adapter — collect payment
        invoker.executeCommand(new PayOrderCommand(order, paymentGateway));

        // Observer + Strategy — fulfill and increment loyalty
        invoker.executeCommand(new FulfillOrderCommand(order));
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
    public Order reorder(Order previousOrder) {
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
    public Order reorderForAnotherCustomer(Order previousOrder, Customer newCustomer) {
        Order clonedOrder = previousOrder.cloneOrder(newCustomer);
        invoker.executeCommand(new PlaceOrderCommand(clonedOrder));
        processOrder(clonedOrder);
        return clonedOrder;
    }

    // ===============================================================
    // Payment Gateway Management (Adapter)
    // ===============================================================

    public void setPaymentGateway(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
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
    public List<Order> getAllOrders() {
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
    public OrderInvoker getInvoker() {
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
    private Coffee applyExtras(Coffee coffee, String... extras) {
        for (String extra : extras) {
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
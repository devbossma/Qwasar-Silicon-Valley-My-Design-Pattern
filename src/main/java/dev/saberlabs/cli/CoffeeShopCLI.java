package dev.saberlabs.cli;

import dev.saberlabs.adapter.*;
import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.decorator.SugarDecorator;
import dev.saberlabs.decorator.WhippedCreamDecorator;
import dev.saberlabs.facade.CoffeeShopFacade;
import dev.saberlabs.factory.CappuccinoCreator;
import dev.saberlabs.factory.CoffeeCreator;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.factory.LatteCreator;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Interactive Coffee Shop CLI — simulates a full coffee machine experience.
 * Demonstrates all 10 design patterns working together through the Facade.
 */
public class CoffeeShopCLI {

    private static final String SEPARATOR = "════════════════════════════════════════════════════════";
    private static final String THIN_SEP  = "────────────────────────────────────────────────────────";

    private final Scanner scanner;
    private final CoffeeShopFacade facade;
    private final Map<String, Customer> customers;
    private Customer currentCustomer;

    public CoffeeShopCLI() {
        this.scanner = new Scanner(System.in);
        this.facade = new CoffeeShopFacade(null); // no default gateway — set at checkout
        this.customers = new HashMap<>();
        this.currentCustomer = null;
    }

    static void main(String[] args) {
        CoffeeShopCLI cli = new CoffeeShopCLI();
        cli.run();
    }

    // ================================================================
    // Main Loop
    // ================================================================

    public void run() {
        printBanner();

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readInt("Your choice: ");

            switch (choice) {
                case 1 -> handleNewCustomer();
                case 2 -> handleSwitchCustomer();
                case 3 -> handleNewOrder();
                case 4 -> handleReorder();
                case 5 -> handleReorderForAnother();
                case 6 -> handleViewOrders();
                case 7 -> handleViewCustomerInfo();
                case 8 -> handleUndoLastAction();
                case 9 -> handleViewCommandHistory();
                case 0 -> {
                    printGoodbye();
                    running = false;
                }
                default -> System.out.println("  Invalid choice. Try again.\n");
            }
        }

        scanner.close();
    }

    // ================================================================
    // Menu Display
    // ================================================================

    private void printBanner() {
        System.out.println();
        System.out.println(SEPARATOR);
        System.out.println("   ☕  QWASAR COFFEE SHOP  ☕");
        System.out.println("   Design Patterns in Action");
        System.out.println(SEPARATOR);
        System.out.println();
    }

    private void printMainMenu() {
        System.out.println(THIN_SEP);
        if (currentCustomer != null) {
            System.out.printf("  Current Customer: %s (%s)%n",
                    currentCustomer.getName(), currentCustomer.getLoyaltyTier());
        } else {
            System.out.println("  No customer selected");
        }
        System.out.println(THIN_SEP);
        System.out.println("  1. New Customer");
        System.out.println("  2. Switch Customer");
        System.out.println("  3. New Order");
        System.out.println("  4. Reorder (same customer)");
        System.out.println("  5. Reorder for another customer");
        System.out.println("  6. View All Orders");
        System.out.println("  7. View Customer Info");
        System.out.println("  8. Undo Last Action");
        System.out.println("  9. View Command History");
        System.out.println("  0. Exit");
        System.out.println(THIN_SEP);
    }

    private void printGoodbye() {
        System.out.println();
        System.out.println(SEPARATOR);
        System.out.printf("  Total orders served: %d%n", facade.getOrderCount());
        System.out.println("  Thank you for visiting Saber Labs Coffee Shop!");
        System.out.println(SEPARATOR);
        System.out.println();
    }

    // ================================================================
    // 1. New Customer
    // ================================================================

    private void handleNewCustomer() {
        System.out.println();
        System.out.println("  ── New Customer ──");
        String name = readString("  Enter customer name: ");

        Customer customer = facade.createCustomer(name);
        customers.put(customer.getId(), customer);
        currentCustomer = customer;

        System.out.printf("  ✓ Customer created: %s (ID: %s)%n%n", customer.getName(), customer.getId());
    }

    // ================================================================
    // 2. Switch Customer
    // ================================================================

    private void handleSwitchCustomer() {
        if (customers.isEmpty()) {
            System.out.println("  No customers registered. Create one first.\n");
            return;
        }

        System.out.println();
        System.out.println("  ── Switch Customer ──");
        List<Customer> customerList = new ArrayList<>(customers.values());
        for (int i = 0; i < customerList.size(); i++) {
            Customer c = customerList.get(i);
            System.out.printf("  %d. %s (%s, %d orders)%n",
                    i + 1, c.getName(), c.getLoyaltyTier(), c.getTotalOrders());
        }

        int choice = readInt("  Select customer: ");
        if (choice < 1 || choice > customerList.size()) {
            System.out.println("  Invalid selection.\n");
            return;
        }

        currentCustomer = customerList.get(choice - 1);
        System.out.printf("  ✓ Switched to: %s%n%n", currentCustomer.getName());
    }

    // ================================================================
    // 3. New Order
    // ================================================================

    private void handleNewOrder() {
        if (currentCustomer == null) {
            System.out.println("  No customer selected. Create or switch first.\n");
            return;
        }

        System.out.println();
        System.out.println("  ── New Order ──");

        // Step 1: Choose coffee type (Factory Method)
        CoffeeCreator creator = chooseCoffeeType();
        if (creator == null) return;

        // Step 2: Choose extras (Decorator)
        Coffee coffee = creator.createCoffee();
        coffee = chooseExtras(coffee);

        // Step 3: Review order
        System.out.println();
        System.out.println("  ── Order Summary ──");
        System.out.printf("  Customer:    %s (%s)%n", currentCustomer.getName(), currentCustomer.getLoyaltyTier());
        System.out.printf("  Coffee:      %s%n", coffee.getDescription());
        System.out.printf("  Base Price:  $%.2f%n", coffee.getCost());

        double discountedPrice = currentCustomer.getLoyaltyTier().getStrategy().calculatePrice(coffee.getCost());
        if (discountedPrice < coffee.getCost()) {
            System.out.printf("  Discount:    %s%n", currentCustomer.getLoyaltyTier().getStrategy().getDescription());
            System.out.printf("  Final Price: $%.2f%n", discountedPrice);
        }

        System.out.println(THIN_SEP);
        String confirm = readString("  Confirm order? (y/n): ");
        if (!confirm.equalsIgnoreCase("y")) {
            System.out.println("  Order cancelled.\n");
            return;
        }

        // Step 4: Place order (Facade → Factory + Decorator + Strategy + Singleton + Observer)
        Order order = facade.placeOrder(currentCustomer, coffee);

        // Step 5: Choose payment method (Adapter)
        PaymentGateway gateway = choosePaymentMethod(order.getFinalPrice());
        if (gateway == null) {
            System.out.println("  Payment cancelled. Order remains placed but unprocessed.\n");
            return;
        }
        facade.setPaymentGateway(gateway);

        // Step 6: Process order (Facade → Command + Template Method + Adapter + Observer)
        try {
            facade.processOrder(order);
            System.out.println();
            System.out.println("  ✓ Order complete!");
            System.out.printf("  %s — %s — $%.2f%n",
                    currentCustomer.getName(), order.getCoffee().getDescription(), order.getFinalPrice());
            System.out.printf("  Loyalty Tier: %s (%d total orders)%n%n",
                    currentCustomer.getLoyaltyTier(), currentCustomer.getTotalOrders());
        } catch (RuntimeException e) {
            System.out.println("  ✗ " + e.getMessage() + "\n");
        }
    }

    // ================================================================
    // 4. Reorder (Prototype — same customer)
    // ================================================================

    private void handleReorder() {
        if (currentCustomer == null) {
            System.out.println("  No customer selected.\n");
            return;
        }

        List<Order> customerOrders = getCustomerOrders(currentCustomer);
        if (customerOrders.isEmpty()) {
            System.out.println("  No previous orders to reorder.\n");
            return;
        }

        System.out.println();
        System.out.println("  ── Reorder (Prototype) ──");
        printOrderList(customerOrders);

        int choice = readInt("  Select order to clone: ");
        if (choice < 1 || choice > customerOrders.size()) {
            System.out.println("  Invalid selection.\n");
            return;
        }

        Order originalOrder = customerOrders.get(choice - 1);

        // Payment for the reorder
        PaymentGateway gateway = choosePaymentMethod(originalOrder.getFinalPrice());
        if (gateway == null) {
            System.out.println("  Payment cancelled.\n");
            return;
        }
        facade.setPaymentGateway(gateway);

        try {
            Order clonedOrder = facade.reorder(originalOrder);
            System.out.println();
            System.out.println("  ✓ Reorder complete!");
            System.out.printf("  Cloned: %s — $%.2f%n%n",
                    clonedOrder.getCoffee().getDescription(), clonedOrder.getFinalPrice());
        } catch (RuntimeException e) {
            System.out.println("  ✗ " + e.getMessage() + "\n");
        }
    }

    // ================================================================
    // 5. Reorder for Another Customer (Prototype — different customer)
    // ================================================================

    private void handleReorderForAnother() {
        if (customers.size() < 2) {
            System.out.println("  Need at least 2 customers. Create another first.\n");
            return;
        }

        System.out.println();
        System.out.println("  ── Reorder for Another Customer ──");

        // Pick the source order
        System.out.println("  Select source customer:");
        List<Customer> customerList = new ArrayList<>(customers.values());
        for (int i = 0; i < customerList.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, customerList.get(i).getName());
        }
        int srcChoice = readInt("  Source customer: ");
        if (srcChoice < 1 || srcChoice > customerList.size()) {
            System.out.println("  Invalid selection.\n");
            return;
        }
        Customer sourceCustomer = customerList.get(srcChoice - 1);

        List<Order> sourceOrders = getCustomerOrders(sourceCustomer);
        if (sourceOrders.isEmpty()) {
            System.out.printf("  %s has no orders to clone.%n%n", sourceCustomer.getName());
            return;
        }

        printOrderList(sourceOrders);
        int orderChoice = readInt("  Select order to clone: ");
        if (orderChoice < 1 || orderChoice > sourceOrders.size()) {
            System.out.println("  Invalid selection.\n");
            return;
        }
        Order originalOrder = sourceOrders.get(orderChoice - 1);

        // Pick the target customer
        System.out.println("  Select target customer:");
        List<Customer> targets = customerList.stream()
                .filter(c -> !c.equals(sourceCustomer))
                .toList();
        for (int i = 0; i < targets.size(); i++) {
            System.out.printf("  %d. %s (%s)%n", i + 1, targets.get(i).getName(), targets.get(i).getLoyaltyTier());
        }
        int tgtChoice = readInt("  Target customer: ");
        if (tgtChoice < 1 || tgtChoice > targets.size()) {
            System.out.println("  Invalid selection.\n");
            return;
        }
        Customer targetCustomer = targets.get(tgtChoice - 1);

        // Payment
        PaymentGateway gateway = choosePaymentMethod(originalOrder.getCoffee().getCost());
        if (gateway == null) {
            System.out.println("  Payment cancelled.\n");
            return;
        }
        facade.setPaymentGateway(gateway);

        try {
            Order clonedOrder = facade.reorderForAnotherCustomer(originalOrder, targetCustomer);
            System.out.println();
            System.out.printf("  ✓ %s got %s's order: %s — $%.2f%n%n",
                    targetCustomer.getName(), sourceCustomer.getName(),
                    clonedOrder.getCoffee().getDescription(), clonedOrder.getFinalPrice());
        } catch (RuntimeException e) {
            System.out.println("  ✗ " + e.getMessage() + "\n");
        }
    }

    // ================================================================
    // 6. View All Orders
    // ================================================================

    private void handleViewOrders() {
        List<Order> orders = facade.getAllOrders();
        if (orders.isEmpty()) {
            System.out.println("  No orders yet.\n");
            return;
        }

        System.out.println();
        System.out.println("  ── All Orders ──");
        System.out.printf("  %-6s %-12s %-30s %-10s %-10s%n",
                "No.", "Customer", "Coffee", "Price", "Status");
        System.out.println("  " + THIN_SEP);

        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            System.out.printf("  %-6d %-12s %-30s $%-9.2f %-10s%n",
                    i + 1,
                    o.getCustomer().getName(),
                    o.getCoffee().getDescription(),
                    o.getFinalPrice(),
                    o.getStatus());
        }
        System.out.println();
    }

    // ================================================================
    // 7. View Customer Info
    // ================================================================

    private void handleViewCustomerInfo() {
        if (customers.isEmpty()) {
            System.out.println("  No customers registered.\n");
            return;
        }

        System.out.println();
        System.out.println("  ── Customer Directory ──");
        System.out.printf("  %-6s %-15s %-10s %-8s%n", "ID", "Name", "Tier", "Orders");
        System.out.println("  " + THIN_SEP);

        for (Customer c : customers.values()) {
            System.out.printf("  %-6s %-15s %-10s %-8d%n",
                    c.getId(), c.getName(), c.getLoyaltyTier(), c.getTotalOrders());
        }
        System.out.println();
    }

    // ================================================================
    // 8. Undo Last Action
    // ================================================================

    private void handleUndoLastAction() {
        System.out.println();
        facade.undoLastAction();
        System.out.println("  ✓ Last action undone.\n");
    }

    // ================================================================
    // 9. View Command History
    // ================================================================

    private void handleViewCommandHistory() {
        var history = facade.getInvoker().getCommandHistory();
        if (history.isEmpty()) {
            System.out.println("  No commands executed yet.\n");
            return;
        }

        System.out.println();
        System.out.println("  ── Command History ──");
        for (int i = 0; i < history.size(); i++) {
            System.out.printf("  %d. [ID: %s] %s%n",
                    i + 1,
                    history.get(i).getCommandName(),
                    history.get(i).getClass().getSimpleName());
        }
        System.out.println();
    }

    // ================================================================
    // Coffee Selection (Factory Method)
    // ================================================================

    private CoffeeCreator chooseCoffeeType() {
        System.out.println("  Select coffee type:");
        System.out.println("  1. Espresso      ($2.50)");
        System.out.println("  2. Cappuccino    ($3.50)");
        System.out.println("  3. Latte         ($4.00)");

        int choice = readInt("  Your choice: ");

        return switch (choice) {
            case 1 -> new EspressoCreator();
            case 2 -> new CappuccinoCreator();
            case 3 -> new LatteCreator();
            default -> {
                System.out.println("  Invalid coffee type.\n");
                yield null;
            }
        };
    }

    // ================================================================
    // Extras Selection (Decorator)
    // ================================================================

    private Coffee chooseExtras(Coffee coffee) {
        System.out.println();
        System.out.println("  Add extras (enter numbers separated by spaces, or 0 for none):");
        System.out.println("  1. Milk           (+$0.50)");
        System.out.println("  2. Sugar          (+$0.25)");
        System.out.println("  3. Whipped Cream  (+$0.75)");

        String input = readString("  Your choice: ").trim();

        if (input.equals("0") || input.isEmpty()) {
            return coffee;
        }

        String[] choices = input.split("\\s+");
        for (String c : choices) {
            coffee = switch (c.trim()) {
                case "1" -> new MilkDecorator(coffee);
                case "2" -> new SugarDecorator(coffee);
                case "3" -> new WhippedCreamDecorator(coffee);
                default -> {
                    System.out.println("  Skipping unknown extra: " + c);
                    yield coffee;
                }
            };
        }

        System.out.printf("  → %s — $%.2f%n", coffee.getDescription(), coffee.getCost());
        return coffee;
    }

    // ================================================================
    // Payment Selection (Adapter)
    // ================================================================

    private PaymentGateway choosePaymentMethod(double amount) {
        System.out.println();
        System.out.printf("  Amount due: $%.2f%n", amount);
        System.out.println("  Select payment method:");
        System.out.println("  1. Cash");
        System.out.println("  2. PayPal");
        System.out.println("  3. Credit Card (Stripe)");
        System.out.println("  0. Cancel");

        int choice = readInt("  Your choice: ");

        return switch (choice) {
            case 1 -> setupCashPayment(amount);
            case 2 -> setupPayPalPayment();
            case 3 -> setupStripePayment();
            case 0 -> null;
            default -> {
                System.out.println("  Invalid payment method.");
                yield null;
            }
        };
    }

    private PaymentGateway setupCashPayment(double amountDue) {
        System.out.printf("  Amount due: $%.2f%n", amountDue);
        double received = readDouble();

        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(received);
        CashPaymentAdapter adapter = new CashPaymentAdapter(cashService);

        // Preview change before processing
        if (received >= amountDue) {
            System.out.printf("  Change to return: $%.2f%n", received - amountDue);
        }

        return adapter;
    }

    private PaymentGateway setupPayPalPayment() {
        String email = readString("  PayPal email: ");
        String password = readString("  PayPal password: ");

        PayPalPaymentService paypalService = new PayPalPaymentService(email, password);
        return new PayPalAdapter(paypalService);
    }

    private PaymentGateway setupStripePayment() {
        String cardNumber = readString("  Card number (16 digits): ");
        String cardHolder = readString("  Cardholder name: ");
        String expMonth = readString("  Expiry month (MM): ");
        String expYear = readString("  Expiry year (YYYY): ");
        String cvv = readString("  CVV (3 digits): ");

        StripePaymentService stripeService = new StripePaymentService(
                cardNumber, cardHolder, expMonth, expYear, cvv);
        return new StripeAdapter(stripeService);
    }

    // ================================================================
    // Helpers
    // ================================================================

    private List<Order> getCustomerOrders(Customer customer) {
        return facade.getAllOrders().stream()
                .filter(o -> o.getCustomer().equals(customer))
                .toList();
    }

    private void printOrderList(List<Order> orders) {
        for (int i = 0; i < orders.size(); i++) {
            Order o = orders.get(i);
            System.out.printf("  %d. %s — $%.2f (%s)%n",
                    i + 1, o.getCoffee().getDescription(), o.getFinalPrice(), o.getStatus());
        }
    }

    private String readString(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private int readInt(String prompt) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private double readDouble() {
        System.out.print("  Cash received: $");
        try {
            return Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
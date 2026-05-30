package dev.saberlabs.adapter;

import dev.saberlabs.command.OrderInvoker;
import dev.saberlabs.command.PayOrderCommand;
import dev.saberlabs.command.PlaceOrderCommand;
import dev.saberlabs.command.PrepareOrderCommand;
import dev.saberlabs.command.FulfillOrderCommand;
import dev.saberlabs.decorator.MilkDecorator;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.singleton.CoffeeShop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adapter Pattern")
class AdapterTest {

    @BeforeEach
    void setUp() {
        CoffeeShop.getInstance().clearOrders();
    }

    // ---- PayPal Adapter Tests ----

    @Test
    @DisplayName("PayPal adapter processes payment successfully")
    void paypalProcessesPayment() {
        PayPalPaymentService paypalService = new PayPalPaymentService("alice@mail.com", "pass");
        PaymentGateway gateway = new PayPalAdapter(paypalService);

        boolean result = gateway.processPayment("ORDER-001", 5.00);

        assertTrue(result);
        assertEquals(PaymentStatus.PAYMENT_COMPLETE, gateway.getPaymentStatus("ORDER-001"));
    }

    @Test
    @DisplayName("PayPal adapter fails on insufficient funds")
    void paypalInsufficientFunds() {
        PayPalPaymentService paypalService = new PayPalPaymentService("alice@mail.com", "pass");
        paypalService.setBalance(1.00);
        PaymentGateway gateway = new PayPalAdapter(paypalService);

        boolean result = gateway.processPayment("ORDER-001", 5.00);

        assertFalse(result);
        assertEquals(PaymentStatus.PAYMENT_FAILED, gateway.getPaymentStatus("ORDER-001"));
    }

    @Test
    @DisplayName("PayPal adapter converts dollars to cents correctly")
    void paypalDollarToCentsConversion() {
        PayPalPaymentService paypalService = new PayPalPaymentService("alice@mail.com", "pass");
        paypalService.setBalance(10.00);
        PaymentGateway gateway = new PayPalAdapter(paypalService);

        gateway.processPayment("ORDER-001", 3.75);

        assertEquals(6.25, paypalService.getBalance(), 0.001);
    }

    @Test
    @DisplayName("PayPal adapter tracks multiple transactions independently")
    void paypalMultipleTransactions() {
        PayPalPaymentService paypalService = new PayPalPaymentService("alice@mail.com", "pass");
        PaymentGateway gateway = new PayPalAdapter(paypalService);

        gateway.processPayment("ORDER-001", 5.00);
        paypalService.setBalance(0.50);
        gateway.processPayment("ORDER-002", 10.00);

        assertEquals(PaymentStatus.PAYMENT_COMPLETE, gateway.getPaymentStatus("ORDER-001"));
        assertEquals(PaymentStatus.PAYMENT_FAILED, gateway.getPaymentStatus("ORDER-002"));
    }

    @Test
    @DisplayName("PayPal adapter throws RuntimeException for non-existent order")
    void paypalUnknownOrder() {
        PayPalPaymentService paypalService = new PayPalPaymentService("alice@mail.com", "pass");
        PaymentGateway gateway = new PayPalAdapter(paypalService);

        assertThrows(RuntimeException.class, () -> gateway.getPaymentStatus("UNKNOWN"));
    }

    // ---- Stripe Adapter Tests ----

    @Test
    @DisplayName("Stripe adapter processes payment successfully")
    void stripeProcessesPayment() {
        StripePaymentService stripeService = new StripePaymentService(
                "1234567890123456", "Bob Smith", "12", "2028", "456");
        PaymentGateway gateway = new StripeAdapter(stripeService);

        boolean result = gateway.processPayment("ORDER-001", 5.00);

        assertTrue(result);
        assertEquals(PaymentStatus.PAYMENT_COMPLETE, gateway.getPaymentStatus("ORDER-001"));
    }

    @Test
    @DisplayName("Stripe adapter fails on insufficient funds")
    void stripeInsufficientFunds() {
        StripePaymentService stripeService = new StripePaymentService(
                "1234567890123456", "Bob Smith", "12", "2028", "456");
        stripeService.setBalance(1.00);
        PaymentGateway gateway = new StripeAdapter(stripeService);

        boolean result = gateway.processPayment("ORDER-001", 5.00);

        assertFalse(result);
        assertEquals(PaymentStatus.PAYMENT_FAILED, gateway.getPaymentStatus("ORDER-001"));
    }

    @Test
    @DisplayName("Stripe adapter fails on invalid card number")
    void stripeInvalidCardNumber() {
        StripePaymentService stripeService = new StripePaymentService(
                "123", "Bob Smith", "12", "2028", "456");
        PaymentGateway gateway = new StripeAdapter(stripeService);

        boolean result = gateway.processPayment("ORDER-001", 5.00);

        assertFalse(result);
        assertEquals(PaymentStatus.PAYMENT_FAILED, gateway.getPaymentStatus("ORDER-001"));
    }

    @Test
    @DisplayName("Stripe adapter fails on expired card")
    void stripeExpiredCard() {
        StripePaymentService stripeService = new StripePaymentService(
                "1234567890123456", "Bob Smith", "01", "2020", "456");
        PaymentGateway gateway = new StripeAdapter(stripeService);

        boolean result = gateway.processPayment("ORDER-001", 5.00);

        assertFalse(result);
        assertEquals(PaymentStatus.PAYMENT_FAILED, gateway.getPaymentStatus("ORDER-001"));
    }

    @Test
    @DisplayName("Stripe adapter fails on invalid CVV")
    void stripeInvalidCvv() {
        StripePaymentService stripeService = new StripePaymentService(
                "1234567890123456", "Bob Smith", "12", "2028", "1");
        PaymentGateway gateway = new StripeAdapter(stripeService);

        boolean result = gateway.processPayment("ORDER-001", 5.00);

        assertFalse(result);
        assertEquals(PaymentStatus.PAYMENT_FAILED, gateway.getPaymentStatus("ORDER-001"));
    }

    @Test
    @DisplayName("Stripe adapter tracks multiple transactions independently")
    void stripeMultipleTransactions() {
        StripePaymentService stripeService = new StripePaymentService(
                "1234567890123456", "Bob Smith", "12", "2028", "456");
        PaymentGateway gateway = new StripeAdapter(stripeService);

        gateway.processPayment("ORDER-001", 5.00);
        stripeService.setBalance(0.50);
        gateway.processPayment("ORDER-002", 10.00);

        assertEquals(PaymentStatus.PAYMENT_COMPLETE, gateway.getPaymentStatus("ORDER-001"));
        assertEquals(PaymentStatus.PAYMENT_FAILED, gateway.getPaymentStatus("ORDER-002"));
    }

    @Test
    @DisplayName("Stripe adapter throws RuntimeException for non-existent order")
    void stripeUnknownOrder() {
        StripePaymentService stripeService = new StripePaymentService(
                "1234567890123456", "Bob Smith", "12", "2028", "456");
        PaymentGateway gateway = new StripeAdapter(stripeService);

        assertThrows(RuntimeException.class, () -> gateway.getPaymentStatus("UNKNOWN"));
    }

    // ---- Adapter Interchangeability Tests ----

    @Test
    @DisplayName("both adapters work through the same PaymentGateway interface")
    void adaptersAreInterchangeable() {
        PaymentGateway paypal = new PayPalAdapter(
                new PayPalPaymentService("alice@mail.com", "pass"));
        PaymentGateway stripe = new StripeAdapter(
                new StripePaymentService("1234567890123456", "Bob", "12", "2028", "456"));

        assertTrue(paypal.processPayment("ORDER-001", 3.50));
        assertTrue(stripe.processPayment("ORDER-002", 4.25));

        assertEquals(PaymentStatus.PAYMENT_COMPLETE, paypal.getPaymentStatus("ORDER-001"));
        assertEquals(PaymentStatus.PAYMENT_COMPLETE, stripe.getPaymentStatus("ORDER-002"));
    }

    @Test
    @DisplayName("PayOrderCommand works with PayPal adapter")
    void payOrderCommandWithPayPal() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 28);
        PaymentGateway paypal = new PayPalAdapter(
                new PayPalPaymentService("alice@mail.com", "pass"));

        PayOrderCommand cmd = new PayOrderCommand(order, paypal);
        cmd.execute();

        assertTrue(cmd.isPaid());
    }

    @Test
    @DisplayName("PayOrderCommand works with Stripe adapter")
    void payOrderCommandWithStripe() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 29);
        PaymentGateway stripe = new StripeAdapter(
                new StripePaymentService("1234567890123456", "Alice", "12", "2028", "456"));

        PayOrderCommand cmd = new PayOrderCommand(order, stripe);
        cmd.execute();

        assertTrue(cmd.isPaid());
    }

    @Test
    @DisplayName("PayOrderCommand throws on payment failure")
    void payOrderCommandFailsOnInsufficientFunds() {
        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 30);
        PayPalPaymentService paypalService = new PayPalPaymentService("alice@mail.com", "pass");
        paypalService.setBalance(0.01);
        PaymentGateway paypal = new PayPalAdapter(paypalService);

        PayOrderCommand cmd = new PayOrderCommand(order, paypal);

        assertThrows(RuntimeException.class, cmd::execute);
        assertFalse(cmd.isPaid());
    }

    // ---- Cash Adapter Tests ----

    @Test
    @DisplayName("Cash adapter processes exact payment successfully")
    void cashExactPayment() {
        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(3.50);
        PaymentGateway gateway = new CashPaymentAdapter(cashService);

        boolean result = gateway.processPayment("ORDER-001", 3.50);

        assertTrue(result);
        assertEquals(PaymentStatus.PAYMENT_COMPLETE, gateway.getPaymentStatus("ORDER-001"));
        assertEquals(3.50, cashService.getCashRegisterTotal(), 0.001);
    }

    @Test
    @DisplayName("Cash adapter calculates correct change on overpayment")
    void cashOverpaymentChange() {
        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(10.00);
        CashPaymentAdapter adapter = new CashPaymentAdapter(cashService);

        adapter.processPayment("ORDER-001", 3.50);

        assertEquals(6.50, adapter.getChange("ORDER-001"), 0.001);
        assertEquals(PaymentStatus.PAYMENT_COMPLETE, adapter.getPaymentStatus("ORDER-001"));
        assertEquals(3.50, cashService.getCashRegisterTotal(), 0.001);
    }

    @Test
    @DisplayName("Cash adapter returns zero change for exact payment")
    void cashExactPaymentZeroChange() {
        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(3.50);
        CashPaymentAdapter adapter = new CashPaymentAdapter(cashService);

        adapter.processPayment("ORDER-001", 3.50);

        assertEquals(0.00, adapter.getChange("ORDER-001"), 0.001);
    }

    @Test
    @DisplayName("Cash adapter fails on insufficient cash")
    void cashInsufficientPayment() {
        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(2.00);
        CashPaymentAdapter adapter = new CashPaymentAdapter(cashService);

        boolean result = adapter.processPayment("ORDER-001", 5.00);

        assertFalse(result);
        assertEquals(PaymentStatus.PAYMENT_FAILED, adapter.getPaymentStatus("ORDER-001"));
        assertEquals(0.00, adapter.getChange("ORDER-001"), 0.001);
        assertEquals(0.00, cashService.getCashRegisterTotal(), 0.001);
    }

    @Test
    @DisplayName("Cash adapter tracks multiple transactions independently")
    void cashMultipleTransactions() {
        CashPaymentService cashService = new CashPaymentService();
        CashPaymentAdapter adapter = new CashPaymentAdapter(cashService);

        cashService.setAmountReceived(5.00);
        adapter.processPayment("ORDER-001", 2.50);

        cashService.setAmountReceived(10.00);
        adapter.processPayment("ORDER-002", 4.00);

        assertEquals(PaymentStatus.PAYMENT_COMPLETE, adapter.getPaymentStatus("ORDER-001"));
        assertEquals(PaymentStatus.PAYMENT_COMPLETE, adapter.getPaymentStatus("ORDER-002"));
        assertEquals(2.50, adapter.getChange("ORDER-001"), 0.001);
        assertEquals(6.00, adapter.getChange("ORDER-002"), 0.001);
        assertEquals(6.50, cashService.getCashRegisterTotal(), 0.001);
    }

    @Test
    @DisplayName("Cash adapter returns PAYMENT_FAILED for unknown order")
    void cashUnknownOrder() {
        CashPaymentService cashService = new CashPaymentService();
        PaymentGateway gateway = new CashPaymentAdapter(cashService);

        assertEquals(PaymentStatus.PAYMENT_FAILED, gateway.getPaymentStatus("UNKNOWN"));
    }

    @Test
    @DisplayName("Cash adapter returns zero change for unknown order")
    void cashUnknownOrderZeroChange() {
        CashPaymentService cashService = new CashPaymentService();
        CashPaymentAdapter adapter = new CashPaymentAdapter(cashService);

        assertEquals(0.00, adapter.getChange("UNKNOWN"), 0.001);
    }

    @Test
    @DisplayName("Cash adapter accumulates register total across transactions")
    void cashRegisterAccumulates() {
        CashPaymentService cashService = new CashPaymentService();
        PaymentGateway gateway = new CashPaymentAdapter(cashService);

        cashService.setAmountReceived(3.00);
        gateway.processPayment("ORDER-001", 3.00);

        cashService.setAmountReceived(4.50);
        gateway.processPayment("ORDER-002", 4.50);

        cashService.setAmountReceived(2.25);
        gateway.processPayment("ORDER-003", 2.25);

        assertEquals(9.75, cashService.getCashRegisterTotal(), 0.001);
    }

    @Test
    @DisplayName("Cash adapter is interchangeable with PayPal and Stripe")
    void cashInterchangeableWithOtherAdapters() {
        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(5.00);

        PaymentGateway paypal = new PayPalAdapter(
                new PayPalPaymentService("alice@mail.com", "pass"));
        PaymentGateway stripe = new StripeAdapter(
                new StripePaymentService("1234567890123456", "Bob", "12", "2028", "456"));
        PaymentGateway cash = new CashPaymentAdapter(cashService);

        assertTrue(paypal.processPayment("ORDER-001", 3.50));
        assertTrue(stripe.processPayment("ORDER-002", 4.25));
        assertTrue(cash.processPayment("ORDER-003", 2.75));

        assertEquals(PaymentStatus.PAYMENT_COMPLETE, paypal.getPaymentStatus("ORDER-001"));
        assertEquals(PaymentStatus.PAYMENT_COMPLETE, stripe.getPaymentStatus("ORDER-002"));
        assertEquals(PaymentStatus.PAYMENT_COMPLETE, cash.getPaymentStatus("ORDER-003"));
    }

    @Test
    @DisplayName("PayOrderCommand works with Cash adapter")
    void payOrderCommandWithCash() {
        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(10.00);
        PaymentGateway cashGateway = new CashPaymentAdapter(cashService);

        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 31);

        PayOrderCommand cmd = new PayOrderCommand(order, cashGateway);
        cmd.execute();

        assertTrue(cmd.isPaid());
    }

    @Test
    @DisplayName("PayOrderCommand fails with Cash adapter on insufficient cash")
    void payOrderCommandFailsWithInsufficientCash() {
        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(0.50);
        PaymentGateway cashGateway = new CashPaymentAdapter(cashService);

        Customer alice = new Customer("C001", "Alice");
        Order order = new Order(alice, new Espresso(), 32);

        PayOrderCommand cmd = new PayOrderCommand(order, cashGateway);

        assertThrows(RuntimeException.class, cmd::execute);
        assertFalse(cmd.isPaid());
    }

    @Test
    @DisplayName("Full lifecycle with cash payment through commands")
    void fullLifecycleWithCashPayment() {
        CoffeeShop shop = CoffeeShop.getInstance();
        Customer alice = new Customer("C001", "Alice");
        shop.registerObserver(alice);

        Coffee coffee = new MilkDecorator(new Espresso());
        Order order = new Order(alice, coffee, 33);

        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(5.00);
        PaymentGateway cashGateway = new CashPaymentAdapter(cashService);

        OrderInvoker invoker = new OrderInvoker();
        invoker.executeCommand(new PlaceOrderCommand(order));
        invoker.executeCommand(new PrepareOrderCommand(order));
        invoker.executeCommand(new PayOrderCommand(order, cashGateway));
        invoker.executeCommand(new FulfillOrderCommand(order));

        assertEquals(OrderStatus.FULFILLED, order.getStatus());
        assertEquals(1, alice.getTotalOrders());
        assertEquals(4, invoker.getCommandHistory().size());
        assertEquals(order.getFinalPrice(), cashService.getCashRegisterTotal(), 0.001);
    }
}

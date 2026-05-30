package dev.saberlabs.adapter;

import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;

public class AdapterDemo {

    public static void run() {
        System.out.println("=== 9. ADAPTER — Unified PaymentGateway for PayPal, Stripe, and Cash ===");

        Customer alice = new Customer("C001", "Alice");
        Customer bob   = new Customer("C002", "Bob");
        Customer dany  = new Customer("C003", "Dany");

        Order aliceOrder = new Order(alice, new Espresso(), 1); // $2.50
        Order bobOrder   = new Order(bob,   new Espresso(), 2);
        Order danyOrder  = new Order(dany,  new Espresso(), 3);

        // ── PayPal ──────────────────────────────────────────────────
        PaymentGateway paypal = new PayPalAdapter(
                new PayPalPaymentService("alice@mail.com", "pass"));
        paypal.processPayment("ORDER-A", aliceOrder.getFinalPrice());
        System.out.println("PayPal  status : " + paypal.getPaymentStatus("ORDER-A"));

        // ── Stripe ──────────────────────────────────────────────────
        PaymentGateway stripe = new StripeAdapter(
                new StripePaymentService("1234567890123456", "Bob Smith", "12", "2028", "456"));
        stripe.processPayment("ORDER-B", bobOrder.getFinalPrice());
        System.out.println("Stripe  status : " + stripe.getPaymentStatus("ORDER-B"));

        // ── Cash ────────────────────────────────────────────────────
        CashPaymentService cashService = new CashPaymentService();
        cashService.setAmountReceived(5.00);
        CashPaymentAdapter cash = new CashPaymentAdapter(cashService);
        cash.processPayment("ORDER-C", danyOrder.getFinalPrice());
        System.out.println("Cash    status : " + cash.getPaymentStatus("ORDER-C"));
        System.out.printf("Cash    change : $%.2f%n", cash.getChange("ORDER-C"));
        System.out.printf("Register total : $%.2f%n", cashService.getCashRegisterTotal());

        // ── Interchangeability — same method call, different providers ──
        System.out.println("-- All three implement PaymentGateway --");
        for (PaymentGateway gateway : new PaymentGateway[]{paypal, stripe, cash}) {
            System.out.println("  " + gateway.getClass().getSimpleName());
        }
        System.out.println();
    }
}

package dev.saberlabs.payment;

import dev.saberlabs.adapter.CashPaymentAdapter;
import dev.saberlabs.adapter.PayPalAdapter;
import dev.saberlabs.adapter.PaymentGateway;
import dev.saberlabs.adapter.StripeAdapter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PaymentSetup")
class PaymentSetupTest {

    private Scanner scannerFor(String input) {
        return new Scanner(input);
    }

    @Nested
    @DisplayName("promptForPaymentMethod()")
    class PromptForPaymentMethodTests {

        @Test
        @DisplayName("choice 1 builds a CashPaymentAdapter")
        void choiceOneBuildsCashAdapter() {
            Scanner scanner = scannerFor("1\n5.00\n");
            PaymentGateway gateway = PaymentSetup.promptForPaymentMethod(scanner, 3.50);
            assertInstanceOf(CashPaymentAdapter.class, gateway);
        }

        @Test
        @DisplayName("choice 2 builds a PayPalAdapter")
        void choiceTwoBuildsPayPalAdapter() {
            Scanner scanner = scannerFor("2\nalice@mail.com\nsecret\n");
            PaymentGateway gateway = PaymentSetup.promptForPaymentMethod(scanner, 3.50);
            assertInstanceOf(PayPalAdapter.class, gateway);
        }

        @Test
        @DisplayName("choice 3 builds a StripeAdapter")
        void choiceThreeBuildsStripeAdapter() {
            Scanner scanner = scannerFor("3\n1234567890123456\nBob Smith\n12\n2028\n456\n");
            PaymentGateway gateway = PaymentSetup.promptForPaymentMethod(scanner, 3.50);
            assertInstanceOf(StripeAdapter.class, gateway);
        }

        @Test
        @DisplayName("choice 0 or anything unrecognized returns null (cancel)")
        void unrecognizedChoiceReturnsNull() {
            Scanner scanner = scannerFor("0\n");
            assertNull(PaymentSetup.promptForPaymentMethod(scanner, 3.50));
        }
    }

    @Nested
    @DisplayName("setupCashPayment()")
    class SetupCashPaymentTests {

        @Test
        @DisplayName("sufficient cash succeeds and change is available on the adapter")
        void sufficientCashSucceeds() {
            Scanner scanner = scannerFor("10.00\n");
            PaymentGateway gateway = PaymentSetup.setupCashPayment(scanner, 3.50);

            assertInstanceOf(CashPaymentAdapter.class, gateway);
            CashPaymentAdapter adapter = (CashPaymentAdapter) gateway;
            assertTrue(adapter.processPayment("ORDER-1", 3.50));
            assertEquals(6.50, adapter.getChange("ORDER-1"), 0.001);
        }

        @Test
        @DisplayName("insufficient cash still returns an adapter, but the payment will fail")
        void insufficientCashWarnsButReturnsAdapter() {
            Scanner scanner = scannerFor("1.00\n");
            PaymentGateway gateway = PaymentSetup.setupCashPayment(scanner, 3.50);

            assertNotNull(gateway);
            assertFalse(gateway.processPayment("ORDER-1", 3.50));
        }

        @Test
        @DisplayName("unparsable amount defaults to exact change")
        void unparsableAmountDefaultsToExactChange() {
            Scanner scanner = scannerFor("not-a-number\n");
            PaymentGateway gateway = PaymentSetup.setupCashPayment(scanner, 3.50);

            assertInstanceOf(CashPaymentAdapter.class, gateway);
            CashPaymentAdapter adapter = (CashPaymentAdapter) gateway;
            assertTrue(adapter.processPayment("ORDER-1", 3.50));
            assertEquals(0.00, adapter.getChange("ORDER-1"), 0.001);
        }
    }

    @Nested
    @DisplayName("setupPayPalPayment()")
    class SetupPayPalPaymentTests {

        @Test
        @DisplayName("builds a working PayPalAdapter from the entered credentials")
        void buildsWorkingAdapter() {
            Scanner scanner = scannerFor("alice@mail.com\nsecret\n");
            PaymentGateway gateway = PaymentSetup.setupPayPalPayment(scanner);

            assertInstanceOf(PayPalAdapter.class, gateway);
            assertTrue(gateway.processPayment("ORDER-1", 5.00));
        }
    }

    @Nested
    @DisplayName("setupStripePayment()")
    class SetupStripePaymentTests {

        @Test
        @DisplayName("builds a working StripeAdapter from the entered card details")
        void buildsWorkingAdapter() {
            Scanner scanner = scannerFor("1234567890123456\nBob Smith\n12\n2028\n456\n");
            PaymentGateway gateway = PaymentSetup.setupStripePayment(scanner);

            assertInstanceOf(StripeAdapter.class, gateway);
            assertTrue(gateway.processPayment("ORDER-1", 5.00));
        }
    }
}

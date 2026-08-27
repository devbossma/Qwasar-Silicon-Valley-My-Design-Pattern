package dev.saberlabs.integration;

import dev.saberlabs.adapter.PayPalAdapter;
import dev.saberlabs.adapter.PayPalPaymentService;
import dev.saberlabs.chat.BaristaQueue;
import dev.saberlabs.chat.ChatNotificationService;
import dev.saberlabs.chat.ChatService;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatNotificationRepository;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatOrderRepository;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatRepository;
import dev.saberlabs.chat.repositories.implementations.memory.InMemoryChatSessionRepository;
import dev.saberlabs.facade.CoffeeShopFacade;
import dev.saberlabs.factory.EspressoCreator;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.LoyaltyTier;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.observer.OrderObserver;
import dev.saberlabs.order.OrderService;
import dev.saberlabs.singleton.CoffeeShop;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Coffee Shop Pattern Integration")
class CoffeeShopIntegrationTest {

    @BeforeEach
    void setUp() {
        CoffeeShop.getInstance().clearOrders();
    }

    @Test
    @DisplayName("facade orchestrates all patterns through a full order lifecycle")
    void facadeOrchestratesFullLifecycle() {
        OrderService orderService = new OrderService(
                new PayPalAdapter(new PayPalPaymentService("shop@mail.com", "pass")));
        CoffeeShopFacade facade = new CoffeeShopFacade(orderService, buildChatService(orderService));
        Customer alice = facade.createCustomer("Alice");
        RecordingObserver observer = new RecordingObserver();
        facade.registerCustomer(observer);

        Order order = facade.placeOrder(alice, new EspressoCreator(), "milk", "sugar");
        facade.processOrder(order);
        Order clone = facade.reorder(order);

        assertEquals(OrderStatus.FULFILLED, order.getStatus());
        assertEquals(OrderStatus.FULFILLED, clone.getStatus());
        assertEquals(2, alice.getTotalOrders());
        assertEquals(2, facade.getOrderCount());
        assertEquals(8, facade.getInvoker().getCommandHistory().size());
        assertTrue(observer.notificationCount.get() >= 8);
    }

    @Test
    @DisplayName("concurrent facade order processing keeps order state and loyalty consistent")
    void concurrentFacadeProcessingIsConsistent() throws Exception {
        int orderCount = 12;
        OrderService orderService = new OrderService(
                new PayPalAdapter(new PayPalPaymentService("shop@mail.com", "pass")));
        CoffeeShopFacade facade = new CoffeeShopFacade(orderService, buildChatService(orderService));
        Customer alice = facade.createCustomer("Alice");
        RecordingObserver observer = new RecordingObserver();
        facade.registerCustomer(observer);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Callable<Order>> tasks = new ArrayList<>();

        for (int i = 0; i < orderCount; i++) {
            tasks.add(() -> {
                start.await();
                Order order = facade.placeOrder(alice, new EspressoCreator(), "milk");
                facade.processOrder(order);
                return order;
            });
        }

        List<Future<Order>> futures = new ArrayList<>();
        for (Callable<Order> task : tasks) {
            futures.add(executor.submit(task));
        }

        start.countDown();
        for (Future<Order> future : futures) {
            assertEquals(OrderStatus.FULFILLED, future.get(5, TimeUnit.SECONDS).getStatus());
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        assertEquals(orderCount, facade.getOrderCount());
        assertEquals(orderCount, alice.getTotalOrders());
        assertEquals(LoyaltyTier.GOLD, alice.getLoyaltyTier());
        assertEquals(orderCount * 4, facade.getInvoker().getCommandHistory().size());
        assertEquals(orderCount * 4, observer.notificationCount.get());
    }

    private static @NotNull ChatService buildChatService(@NotNull OrderService orderService) {
        return new ChatService(
                new InMemoryChatRepository(),
                new InMemoryChatSessionRepository(),
                new InMemoryChatOrderRepository(),
                new ChatNotificationService(new InMemoryChatNotificationRepository()),
                new BaristaQueue(),
                orderService);
    }

    private static class RecordingObserver implements OrderObserver {
        private final AtomicInteger notificationCount = new AtomicInteger();

        @Override
        public void update(@NotNull Order order, @NotNull OrderStatus event) {
            notificationCount.incrementAndGet();
        }
    }
}

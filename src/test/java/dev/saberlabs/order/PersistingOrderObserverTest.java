package dev.saberlabs.order;

import dev.saberlabs.chat.ChatNotificationService;
import dev.saberlabs.chat.repositories.ChatOrderRepository;
import dev.saberlabs.models.Coffee;
import dev.saberlabs.models.Customer;
import dev.saberlabs.models.Espresso;
import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PersistingOrderObserver depends on two collaborators it doesn't own —
 * ChatOrderRepository and ChatNotificationService — so both are mocked
 * to isolate the observer's dispatch logic from real persistence/delivery.
 */
@DisplayName("PersistingOrderObserver")
class PersistingOrderObserverTest {

    private ChatOrderRepository orderRepository;
    private ChatNotificationService notificationService;
    private PersistingOrderObserver observer;
    private Order order;

    private static final long CUSTOMER_ID = 42L;
    private static final String ORDER_ID = "ORD-1";

    @BeforeEach
    void setUp() {
        orderRepository = mock(ChatOrderRepository.class);
        notificationService = mock(ChatNotificationService.class);
        observer = new PersistingOrderObserver(orderRepository, notificationService);

        Coffee coffee = new Espresso();
        Customer customer = new Customer("C001", "Alice");
        order = new Order(customer, coffee, ORDER_ID);
    }

    private StoredOrder storedOrder() {
        return new StoredOrder(ORDER_ID, CUSTOMER_ID, null, null,
                "Espresso", List.of(), 2.50, "PREPARING", LocalDateTime.now());
    }

    @Test
    @DisplayName("PLACED events are ignored entirely")
    void placedEventsAreIgnored() {
        observer.update(order, OrderStatus.PLACED);

        verifyNoInteractions(orderRepository, notificationService);
    }

    @Test
    @DisplayName("PREPARING events are ignored entirely")
    void preparingEventsAreIgnored() {
        observer.update(order, OrderStatus.PREPARING);

        verifyNoInteractions(orderRepository, notificationService);
    }

    @Test
    @DisplayName("READY persists the status and notifies the customer the order is ready")
    void readyPersistsAndNotifies() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(storedOrder()));

        observer.update(order, OrderStatus.READY);

        verify(orderRepository).updateStatus(ORDER_ID, "READY");
        verify(notificationService).notifyOrderReady(CUSTOMER_ID, ORDER_ID, "Espresso");
        verify(notificationService, never()).notifyOrderFulfilled(anyLong(), anyString());
    }

    @Test
    @DisplayName("FULFILLED persists the status and notifies the customer of fulfillment")
    void fulfilledPersistsAndNotifies() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(storedOrder()));

        observer.update(order, OrderStatus.FULFILLED);

        verify(orderRepository).updateStatus(ORDER_ID, "FULFILLED");
        verify(notificationService).notifyOrderFulfilled(CUSTOMER_ID, ORDER_ID);
        verify(notificationService, never()).notifyOrderReady(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("CANCELLED persists the status but sends no notification")
    void cancelledPersistsWithoutNotifying() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(storedOrder()));

        observer.update(order, OrderStatus.CANCELLED);

        verify(orderRepository).updateStatus(ORDER_ID, "CANCELLED");
        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("does nothing when the order isn't found in the repository")
    void doesNothingWhenOrderNotFound() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        observer.update(order, OrderStatus.READY);

        verify(orderRepository, never()).updateStatus(anyString(), anyString());
        verifyNoInteractions(notificationService);
    }
}

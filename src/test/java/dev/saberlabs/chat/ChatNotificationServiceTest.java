package dev.saberlabs.chat;

import dev.saberlabs.chat.repositories.ChatNotificationRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ChatNotificationService depends on ChatNotificationRepository (persistence)
 * and NotificationObserver (real-time delivery) -- both collaborators it
 * doesn't own, so both are mocked here to isolate the service's own
 * dispatch/delegation logic. Mirrors the isolation pattern already used for
 * PersistingOrderObserver and ChatService's payment-gateway tests.
 */
@DisplayName("ChatNotificationService")
class ChatNotificationServiceTest {

    private static final long USER_ID = 7L;

    private ChatNotificationRepository repository;
    private ChatNotificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ChatNotificationRepository.class);
        service = new ChatNotificationService(repository);
        when(repository.save(any(ChatNotification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("notify()")
    class NotifyTests {

        @Test
        @DisplayName("saves the notification via the repository")
        void savesViaRepository() {
            service.notify(USER_ID, "Hello", NotificationType.SESSION_MATCHED, "ref-1");

            verify(repository).save(argThat(n ->
                    n.userId() == USER_ID
                            && n.content().equals("Hello")
                            && n.type() == NotificationType.SESSION_MATCHED
                            && n.referenceId().equals("ref-1")
                            && !n.isRead()));
        }

        @Test
        @DisplayName("dispatches the saved notification to every registered observer")
        void dispatchesToObservers() {
            List<ChatNotification> receivedByA = new java.util.ArrayList<>();
            List<ChatNotification> receivedByB = new java.util.ArrayList<>();
            service.registerObserver(receivedByA::add);
            service.registerObserver(receivedByB::add);

            ChatNotification result = service.notify(
                    USER_ID, "Hello", NotificationType.SESSION_MATCHED, "ref-1");

            assertEquals(1, receivedByA.size());
            assertEquals(1, receivedByB.size());
            assertSame(result, receivedByA.get(0));
            assertSame(result, receivedByB.get(0));
        }
    }

    @Nested
    @DisplayName("convenience notification methods")
    class ConvenienceMethodTests {

        @Test
        @DisplayName("notifyOrderReady formats the coffee description and READY type")
        void notifyOrderReady() {
            service.notifyOrderReady(USER_ID, "ORD-1", "Cappuccino + Milk");

            verify(repository).save(argThat(n ->
                    n.type() == NotificationType.ORDER_READY
                            && n.referenceId().equals("ORD-1")
                            && n.content().contains("Cappuccino + Milk")
                            && n.content().contains("ORD-1")));
        }

        @Test
        @DisplayName("notifyOrderFulfilled uses the FULFILLED type and order reference")
        void notifyOrderFulfilled() {
            service.notifyOrderFulfilled(USER_ID, "ORD-2");

            verify(repository).save(argThat(n ->
                    n.type() == NotificationType.ORDER_FULFILLED
                            && n.referenceId().equals("ORD-2")
                            && n.content().contains("ORD-2")));
        }

        @Test
        @DisplayName("notifySessionMatched references the barista and session")
        void notifySessionMatched() {
            service.notifySessionMatched(USER_ID, 99L, 5L);

            verify(repository).save(argThat(n ->
                    n.type() == NotificationType.SESSION_MATCHED
                            && n.referenceId().equals("5")
                            && n.content().contains("99")));
        }

        @Test
        @DisplayName("notifyPaymentReceived includes the order reference and customer name")
        void notifyPaymentReceived() {
            service.notifyPaymentReceived(USER_ID, "ORD-3", 4.25, "Alice");

            verify(repository).save(argThat(n ->
                    n.type() == NotificationType.PAYMENT_RECEIVED
                            && n.referenceId().equals("ORD-3")
                            && n.content().contains("Alice")
                            && n.content().contains("ORD-3")));
        }

        @Test
        @DisplayName("notifySessionEnded references the customer name and session")
        void notifySessionEnded() {
            service.notifySessionEnded(USER_ID, 12L, "Bob");

            verify(repository).save(argThat(n ->
                    n.type() == NotificationType.SESSION_ENDED
                            && n.referenceId().equals("12")
                            && n.content().contains("Bob")));
        }
    }

    @Nested
    @DisplayName("history / unread")
    class HistoryAndUnreadTests {

        @Test
        @DisplayName("getUnread() delegates to repository.findUnreadByUser()")
        void getUnreadDelegates() {
            ChatNotification unread = new ChatNotification(1L, USER_ID, "x",
                    NotificationType.ORDER_READY, "ORD-1", false, LocalDateTime.now());
            when(repository.findUnreadByUser(USER_ID)).thenReturn(List.of(unread));

            assertEquals(List.of(unread), service.getUnread(USER_ID));
        }

        @Test
        @DisplayName("getHistory() delegates to repository.findByUser()")
        void getHistoryDelegates() {
            ChatNotification past = new ChatNotification(2L, USER_ID, "y",
                    NotificationType.ORDER_FULFILLED, "ORD-2", true, LocalDateTime.now());
            when(repository.findByUser(USER_ID)).thenReturn(List.of(past));

            assertEquals(List.of(past), service.getHistory(USER_ID));
        }

        @Test
        @DisplayName("markAllRead() delegates to repository.markAllReadForUser()")
        void markAllReadDelegates() {
            service.markAllRead(USER_ID);

            verify(repository).markAllReadForUser(USER_ID);
        }
    }

    @Nested
    @DisplayName("observer management")
    class ObserverManagementTests {

        @Test
        @DisplayName("removeObserver stops further delivery to that observer")
        void removeObserverStopsDelivery() {
            AtomicInteger callCount = new AtomicInteger();
            NotificationObserver observer = n -> callCount.incrementAndGet();
            service.registerObserver(observer);

            service.notify(USER_ID, "first", NotificationType.SESSION_MATCHED, "ref-1");
            service.removeObserver(observer);
            service.notify(USER_ID, "second", NotificationType.SESSION_MATCHED, "ref-2");

            assertEquals(1, callCount.get());
        }

        @Test
        @DisplayName("removing one observer does not affect the others")
        void removeOneObserverLeavesOthersIntact() {
            AtomicInteger stayingObserverCalls = new AtomicInteger();
            NotificationObserver leaving = n -> { };
            NotificationObserver staying = n -> stayingObserverCalls.incrementAndGet();
            service.registerObserver(leaving);
            service.registerObserver(staying);

            service.removeObserver(leaving);
            service.notify(USER_ID, "hi", NotificationType.SESSION_MATCHED, "ref-1");

            assertEquals(1, stayingObserverCalls.get());
        }
    }
}

package dev.saberlabs.chat;

import dev.saberlabs.models.Order;
import dev.saberlabs.models.OrderStatus;
import dev.saberlabs.observer.OrderObserver;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Pattern: OBSERVER (Concrete Observer)
 * *
 * Bridges the existing Order/Observer pipeline to the chat session
 * lifecycle. When an order placed via chat reaches FULFILLED, this
 * observer locates the owning {@link ChatSession} (via the ChatMessage
 * that announced the order) and calls {@link ChatService#endSession(long)},
 * which marks the session INACTIVE and frees its barista for rematching.
 * *
 * Registered once at startup alongside the existing Customer observers
 * — see {@link dev.saberlabs.singleton.CoffeeShop#registerObserver}.
 */
public class ChatSessionCloser implements OrderObserver {

    @NotNull private final ChatService chatService;
    @NotNull private final ChatRepository chatRepository;

    public ChatSessionCloser(@NotNull ChatService chatService,
                             @NotNull ChatRepository chatRepository) {
        this.chatService = Objects.requireNonNull(chatService, "ChatService cannot be null");
        this.chatRepository = Objects.requireNonNull(chatRepository, "ChatRepository cannot be null");
    }

    @Override
    public void update(@NotNull Order order, @NotNull OrderStatus event) {
        if (event != OrderStatus.FULFILLED) {
            return;
        }

        Optional<ChatMessage> announcement = chatRepository.findFirstByOrderId(order.getOrderId());
        announcement.ifPresent(message -> chatService.endSession(message.sessionId()));
    }
}
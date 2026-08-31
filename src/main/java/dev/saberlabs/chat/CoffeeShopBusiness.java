package dev.saberlabs.chat;

import dev.saberlabs.auth.User;
import dev.saberlabs.framework.BusinessObject;
import dev.saberlabs.framework.annotation.ChatHandler;
import dev.saberlabs.framework.annotation.OrderHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Pattern 11: REFLECTION FRAMEWORK (Real business object)
 *
 * The real, working counterpart to {@code BusinessTestClient}'s toy {@code CoffeeShopBusiness}
 * demo: {@link #handleOrder} and {@link #handleChat} place a real order / send a real chat
 * message through the real {@link ChatService}, instead of appending to a {@code List<String>}.
 * This is the one {@code BusinessObject} for the whole application — a business object stands
 * for a single business, so there's exactly one, matching {@code BookStoreBusiness}/
 * {@code OnlineShopBusiness}'s own shape in the demo.
 * <p>
 * {@link ChatService#processCustomerInput} constructs a fresh instance of this class for every
 * call, scoped to that one customer's one input — {@code user}/{@code session} are captured at
 * construction and never shared or mutated across calls. That's what lets the handler methods
 * below act with a real identity without any walk-in placeholder or mutable "current session"
 * state on {@code ChatService} itself.
 *
 * @see dev.saberlabs.framework.reflection.InteractionHandler
 */
final class CoffeeShopBusiness implements BusinessObject {

    @NotNull private final ChatService chatService;
    @NotNull private final User user;
    @NotNull private final ChatSession session;

    private ChatMessage result;

    CoffeeShopBusiness(@NotNull ChatService chatService, @NotNull User user, @NotNull ChatSession session) {
        this.chatService = Objects.requireNonNull(chatService, "Chat service cannot be null");
        this.user = Objects.requireNonNull(user, "User cannot be null");
        this.session = Objects.requireNonNull(session, "Session cannot be null");
    }

    /**
     * Parses and places a real order through {@link ChatService#handleOrderCommand} — the
     * coffee-specific parsing and persistence stay there; this method is just the annotated
     * seam the framework can find.
     *
     * @param request the raw order command text, e.g. {@code "/order espresso milk"}
     */
    @OrderHandler
    public void handleOrder(String request) {
        result = chatService.handleOrderCommand(user, session, request);
    }

    /**
     * Sends the text as a real chat message through {@link ChatService#sendMessage}.
     *
     * @param request the raw chat text
     */
    @ChatHandler
    public void handleChat(String request) {
        result = chatService.sendMessage(session.id(), user.id(), user.username(), request);
    }

    /**
     * The classification {@link dev.saberlabs.framework.reflection.InteractionHandler} makes is
     * binary (order or chat) and this class annotates both, so this fallback is unreachable in
     * normal operation — kept only to satisfy the {@link BusinessObject} contract, matching
     * {@code BookStoreBusiness}/{@code OnlineShopBusiness}'s own empty implementation in the demo.
     *
     * @param request the request with no dedicated handler
     */
    @Override
    public void processRequest(String request) {
    }

    /**
     * The {@link ChatMessage} produced by whichever handler ran, for {@link ChatService} to
     * return to its own caller.
     *
     * @return the result of dispatching this instance's one request
     */
    @NotNull ChatMessage result() {
        return Objects.requireNonNull(result, "No handler has run for this request yet");
    }
}

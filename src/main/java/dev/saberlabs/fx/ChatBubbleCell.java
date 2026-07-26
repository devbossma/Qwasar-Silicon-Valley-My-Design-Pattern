package dev.saberlabs.fx;

import dev.saberlabs.chat.ChatMessage;
import dev.saberlabs.chat.ImageUpload;
import dev.saberlabs.chat.MessageType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.util.function.Function;

/**
 * Renders a single ChatMessage as a chat bubble.
 * *
 * - Messages from the current user align right, honey-gold bubble
 * - Messages from others align left, roasted-bean bubble
 * - System messages are centered, subdued, no bubble
 * - Messages tagged with an image (via the imageResolver) render a
 *   thumbnail inside the bubble instead of / alongside text
 */
public class ChatBubbleCell extends ListCell<ChatMessage> {

    private final long currentUserId;
    private final Function<ChatMessage, ImageUpload> imageResolver;

    /**
     * @param currentUserId  the logged-in user's ID — determines bubble side
     * @param imageResolver  looks up an ImageUpload for a message, or null
     *                       if the message isn't an image share
     */
    public ChatBubbleCell(long currentUserId, @NotNull Function<ChatMessage, ImageUpload> imageResolver) {
        this.currentUserId = currentUserId;
        this.imageResolver = imageResolver;
        setStyle("-fx-background-color: transparent; -fx-padding: 2 8;");
    }

    @Override
    protected void updateItem(ChatMessage message, boolean empty) {
        super.updateItem(message, empty);

        if (empty || message == null) {
            setGraphic(null);
            setText(null);
            return;
        }

        if (message.type() == MessageType.SYSTEM_MESSAGE) {
            setGraphic(buildSystemBubble(message));
            return;
        }

        boolean isOwn = message.senderId() == currentUserId;
        setGraphic(buildChatBubble(message, isOwn));
    }

    private @NotNull HBox buildSystemBubble(@NotNull ChatMessage message) {
        Label label = new Label(message.content());
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #8a7768; -fx-font-size: 11px; "
                + "-fx-font-style: italic; -fx-padding: 4 12;");

        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER);
        return row;
    }

    private @NotNull HBox buildChatBubble(@NotNull ChatMessage message, boolean isOwn) {
        VBox bubble = new VBox(4);
        bubble.setMaxWidth(360);
        bubble.setPadding(new Insets(8, 12, 8, 12));
        bubble.setStyle(isOwn
                ? "-fx-background-color: #d9a441; -fx-background-radius: 14 14 2 14;"
                : "-fx-background-color: #4a342a; -fx-background-radius: 14 14 14 2;");

        // Sender name (only for received messages — own bubbles don't need it)
        if (!isOwn) {
            Label sender = new Label(message.senderId() == 0 ? "System" : message.senderName());
            sender.setStyle("-fx-text-fill: #d9a441; -fx-font-size: 10px; -fx-font-weight: bold;");
            bubble.getChildren().add(sender);
        }

        // Image attachment, if this message references one
        ImageUpload image = imageResolver.apply(message);
        if (image != null) {
            ImageView thumbnail = new ImageView(
                    new Image(new ByteArrayInputStream(image.data()), 240, 180, true, true));
            thumbnail.setStyle("-fx-background-radius: 8;");
            bubble.getChildren().add(thumbnail);

            Label filenameLabel = new Label("📎 " + image.filename());
            filenameLabel.setStyle("-fx-text-fill: "
                    + (isOwn ? "#5a3f18" : "#c9b8a4") + "; -fx-font-size: 10px;");
            bubble.getChildren().add(filenameLabel);
        }

        // Message text (skip if the content was purely the upload announcement,
        // and we already rendered the image — avoids duplicate "Shared a photo: x.png" text)
        if (image == null || !message.content().startsWith("📎 Shared a photo")) {
            Label content = new Label(message.content());
            content.setWrapText(true);
            content.setStyle("-fx-text-fill: "
                    + (isOwn ? "#2b1d16" : "#e8dcc8")
                    + "; -fx-font-size: 13px;");
            bubble.getChildren().add(content);
        }

        // Timestamp
        Label time = new Label(String.format("%02d:%02d",
                message.timestamp().getHour(), message.timestamp().getMinute()));
        time.setStyle("-fx-text-fill: "
                + (isOwn ? "#5a3f18" : "#8a7768")
                + "; -fx-font-size: 9px;");
        bubble.getChildren().add(time);

        HBox row = new HBox(bubble);
        row.setAlignment(isOwn ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 4, 2, 4));
        return row;
    }
}
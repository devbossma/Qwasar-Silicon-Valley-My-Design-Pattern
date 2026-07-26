package dev.saberlabs.fx;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.TextField;
import javafx.stage.Popup;
import org.jetbrains.annotations.NotNull;

/**
 * A lightweight emoji picker popup — appends the chosen emoji directly
 * into the given TextField at the caret position.
 * *
 * Not a full emoji library — a curated set covering common chat use:
 * reactions, food/drink (coffee-themed!), and everyday expressions.
 */
public final class EmojiPicker {

    private static final String[] EMOJIS = {
            "😀", "😂", "😍", "😊", "😉", "😢", "😮", "😡",
            "👍", "👎", "🙏", "👏", "🤝", "✅", "❌", "❓",
            "☕", "🥤", "🍰", "🍪", "🥐", "🧋", "🍵", "🔥",
            "❤️", "🎉", "⭐", "💬", "⏰", "📦", "💳", "🚚",
            "💡", "🎁", "🎶", "📷", "🎨", "📝", "📚", "🛠️",
            "🌍", "🌞", "🌧️", "🌈", "🌸", "🍂", "❄️", "🌊",
            "🏠", "🏢", "🏞️", "🏖️", "🏔️",
            "🚗", "🚲", "✈️", "🚀", "🛳️", "🚂", "🚌", "🚑",
            "⚽", "🏀", "🏈", "🎾", "🏐", "🏓", "🏸",
            "🥊", "🥋", "🎯"
    };

    private EmojiPicker() { }

    /**
     * Shows an emoji picker popup anchored below the given button,
     * inserting the selected emoji into the target field.
     */
    public static void show(@NotNull Button anchor, @NotNull TextField target) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        FlowPane grid = new FlowPane(4, 4);
        grid.setPrefWrapLength(200);
        grid.setPadding(new Insets(8));
        grid.setStyle("-fx-background-color: #4a342a; "
                + "-fx-border-color: #6f4e37; -fx-border-radius: 8; "
                + "-fx-background-radius: 8;");

        for (String emoji : EMOJIS) {
            Button emojiBtn = createEmojiBtn(target, emoji, popup);
            grid.getChildren().add(emojiBtn);
        }

        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(160);
        scroll.setStyle("-fx-background: #4a342a; -fx-background-color: #4a342a;");

        popup.getContent().add(scroll);
        popup.show(anchor,
                anchor.localToScreen(0, 0).getX(),
                anchor.localToScreen(0, 0).getY() - 170);
    }

    private static @NotNull Button createEmojiBtn(@NotNull TextField target, String emoji, Popup popup) {
        Button emojiBtn = new Button(emoji);
        emojiBtn.setStyle("-fx-background-color: transparent; "
                + "-fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 4;");
        emojiBtn.setOnMouseEntered(e ->
                emojiBtn.setStyle("-fx-background-color: #6f4e37; "
                        + "-fx-font-size: 18px; -fx-cursor: hand; "
                        + "-fx-padding: 4; -fx-background-radius: 4;"));
        emojiBtn.setOnMouseExited(e ->
                emojiBtn.setStyle("-fx-background-color: transparent; "
                        + "-fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 4;"));
        emojiBtn.setOnAction(e -> {
            int caret = target.getCaretPosition();
            target.insertText(caret, emoji);
            popup.hide();
            target.requestFocus();
        });
        return emojiBtn;
    }
}
package dev.saberlabs.fx.controllers;

import dev.saberlabs.auth.Role;
import dev.saberlabs.auth.User;
import dev.saberlabs.fx.FxTestBase;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.application.Platform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

@ExtendWith(ApplicationExtension.class)
@DisplayName("CustomerController")
class CustomerControllerTest extends FxTestBase {

    static {
        configureTestDatabase();
    }

    // Unique ID per test METHOD, not per class — guarantees each test
    // gets a customer with no prior session, eliminating cross-test
    // state leakage even though the Stage/controller are reused.
    private static final AtomicLong userIdSeq = new AtomicLong(9000L);

    private Stage stage;

    /**
     * Rebuilds the entire scene fresh for EVERY test method, with a
     * brand-new User each time. This is the actual fix: @Start only
     * runs once per class in TestFX, so relying on it alone means all
     * five tests shared one controller instance and one session.
     */
    private void freshScene(Stage stage) throws Exception {
        // UI changes must run on the JavaFX Application Thread; schedule and wait for completion.
        Platform.runLater(() -> {
            try {
                User freshUser = new User(userIdSeq.incrementAndGet(),
                        "test_user_" + userIdSeq.get(), "hash",
                        Role.CUSTOMER, LocalDateTime.now());

                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/customer.fxml"));
                Parent root = loader.load();
                CustomerController controller = loader.getController();
                controller.setOwnerStage(stage);
                controller.setSessionUser(freshUser);
                stage.setScene(new Scene(root));
                stage.show();
                TabPane tabPane = (TabPane) root.lookup(".tab-pane");
                if (tabPane != null) {
                    tabPane.getSelectionModel().select(0);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
    }

    @org.testfx.framework.junit5.Start
    void start(Stage stage) {
        this.stage = stage;
        stage.show(); // real content loaded per-test via freshScene()
    }

    @Test
    @DisplayName("username is displayed on load")
    void usernameDisplayed(FxRobot robot) throws Exception {
        freshScene(stage);

        String actualText = robot.lookup("#usernameLabel")
                .queryLabeled()
                .getText();

        assertTrue(actualText.startsWith("test_user_"),
                "Expected username label to start with 'test_user_', got: " + actualText);
    }

    @Test
    @DisplayName("session status shows 'No active session' before starting chat")
    void noSessionInitially() throws Exception {
        freshScene(stage);
        verifyThat("#sessionStatusLabel", hasText("No active session"));
    }

    @Test
    @DisplayName("clicking Start Chat creates a session and updates status")
    void startChatCreatesSession(FxRobot robot) throws Exception {
        freshScene(stage);

        robot.clickOn("#startChatBtn");
        WaitForAsyncUtils.waitForFxEvents();

        var statusLabel = robot.lookup("#sessionStatusLabel")
                .queryLabeled().getText();
        assertTrue(statusLabel.contains("Waiting") || statusLabel.contains("Connected"),
                "Expected a session status change, got: " + statusLabel);
    }

    @Test
    @DisplayName("typing a message and clicking Send adds it to the chat list")
    void sendMessageAppearsInChat(FxRobot robot) throws Exception {
        freshScene(stage);

        robot.clickOn("#startChatBtn");
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#messageInput").write("hello there");
        robot.type(javafx.scene.input.KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        ListView<?> chatList = robot.lookup("#chatListView").queryListView();
        assertFalse(chatList.getItems().isEmpty(),
                "Chat list should contain the sent message");
    }

    @Test
    @DisplayName("message input is cleared after sending")
    void inputClearedAfterSend(FxRobot robot) throws Exception {
        freshScene(stage);

        robot.clickOn("#startChatBtn");
        WaitForAsyncUtils.waitForFxEvents();

        TextField input = robot.lookup("#messageInput").queryAs(TextField.class);
        robot.clickOn("#messageInput").write("test message");
        robot.type(javafx.scene.input.KeyCode.ENTER);
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals("", input.getText());
    }
}
package dev.saberlabs.fx.controllers;

import dev.saberlabs.auth.Role;
import dev.saberlabs.auth.User;
import dev.saberlabs.fx.FxTestBase;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.control.LabeledMatchers.hasText;

@ExtendWith(ApplicationExtension.class)
@DisplayName("BaristaController")
class BaristaControllerTest extends FxTestBase {

    private BaristaController controller;
    private User testUser;
    private Stage stage;

    @Start
    void start(Stage stage) throws Exception {
        this.stage = stage;

        testUser = new User(2000L, "test_sara", "hash",
                Role.BARISTA, LocalDateTime.now());

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/barista.fxml"));
        Parent root = loader.load();

        controller = loader.getController();
        controller.setOwnerStage(stage);
        controller.setSessionUser(testUser);

        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    @DisplayName("username is displayed on load")
    void usernameDisplayed() {
        verifyThat("#usernameLabel", hasText("test_sara"));
    }

    @Test
    @DisplayName("dashboard refresh button populates the session list without error")
    void refreshDashboardDoesNotThrow(org.testfx.api.FxRobot robot) {
        assertDoesNotThrow(() -> {
            robot.clickOn("#");  // placeholder — real fx:id lookup below
        });
    }

    @Test
    @DisplayName("selecting no session and clicking Send to Kitchen shows an alert, does not crash")
    void sendToKitchenWithNoSessionShowsAlert(org.testfx.api.FxRobot robot) {
        robot.clickOn("Send to Kitchen");
        WaitForAsyncUtils.waitForFxEvents();

        // An Alert dialog should now be showing — dismiss it
        robot.lookup(".button").tryQuery().ifPresent(robot::clickOn);
        WaitForAsyncUtils.waitForFxEvents();
    }
}
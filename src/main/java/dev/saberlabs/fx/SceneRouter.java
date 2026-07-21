package dev.saberlabs.fx;

import dev.saberlabs.CoffeeChatAppFX;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Utility for switching JavaFX scenes from any controller.
 * Controllers call SceneRouter.navigateTo() rather than needing a
 * reference to the primary Stage — the Stage is stored once when
 * CoffeeChatFX.start() runs and reused on every navigation.
 */
public final class SceneRouter {

    private static Stage primaryStage;

    private SceneRouter() { }

    /**
     * Stores the primary Stage on application startup.
     * Called once from {@link CoffeeChatAppFX#start(Stage)}.
     *
     * @param stage the application's primary Stage
     */
    public static void setStage(@NotNull Stage stage) {
        primaryStage = stage;
    }

    /**
     * Loads an FXML file and switches the primary Stage to it.
     *
     * @param fxmlPath the classpath path to the FXML file
     * @param title    the window title to set
     */
    public static void navigateTo(@NotNull String fxmlPath, @NotNull String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    SceneRouter.class.getResource(fxmlPath));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    SceneRouter.class.getResource("/css/style.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scene: " + fxmlPath, e);
        }
    }

    /**
     * Returns to the login screen.
     */
    public static void navigateToLogin() {
        AppContext.getInstance().setCurrentUser(null);
        navigateTo("/fxml/login.fxml", "☕ Coffee Chat");
    }
}
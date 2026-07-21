package dev.saberlabs;

import dev.saberlabs.fx.AppContext;
import dev.saberlabs.fx.SceneRouter;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * JavaFX entry point — Part 2 desktop application.
 *
 * The backend (services, repositories, CoffeeShop, JDBC) is initialized
 * once via {@link AppContext#getInstance()} and shared by all controllers
 * for the lifetime of the application.
 *
 * Part 1 CLI ({@link CoffeeChatAppCLI}) is completely unaffected — both
 * entry points share the same backend; only the UI layer differs.
 *
 * Run with: mvn javafx:run
 */
public class CoffeeChatAppFX extends Application {

    @Override
    public void start(@NotNull Stage primaryStage) throws IOException {
        AppContext.getInstance();               // initialize all services
        SceneRouter.setStage(primaryStage);    // register stage for navigation

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load(), 480, 400);
        scene.getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());

        primaryStage.setTitle("☕ Coffee Chat");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Graceful shutdown — same as CoffeeChatApp.finally block
        AppContext.getInstance().shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
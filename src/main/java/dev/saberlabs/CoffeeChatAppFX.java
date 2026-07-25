package dev.saberlabs;

import dev.saberlabs.fx.AppContext;
import dev.saberlabs.fx.controllers.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

/**
 * JavaFX entry point — Part 2 desktop application.
 * *
 * The backend (services, repositories, CoffeeShop, JDBC) is initialized
 * once via {@link AppContext#getInstance()} and shared by all controllers
 * for the lifetime of the application.
 * *
 * Part 1 CLI ({@link CoffeeChatAppCLI}) is completely unaffected — both
 * entry points share the same backend; only the UI layer differs.
 * *
 * Run with: mvn javafx:run
 */
public class CoffeeChatAppFX extends Application {

    // In CoffeeChatFX.start()

    @Override
    public void start(@NotNull Stage primaryStage) throws IOException {


        AppContext.getInstance();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/login.fxml"));
        Parent root = loader.load();

        LoginController controller = loader.getController();
        controller.setOwnerStage(primaryStage);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());

        primaryStage.setTitle("☕ Coffee Chat");
        primaryStage.setScene(scene);
        primaryStage.sizeToScene();
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    @Override
    public void stop() {
        AppContext.getInstance().shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
package dev.saberlabs.views;

import dev.saberlabs.auth.AuthException;
import dev.saberlabs.auth.AuthService;
import dev.saberlabs.auth.Role;
import dev.saberlabs.auth.User;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Scanner;

/**
 * Console-based login/registration screen — the entry point of the
 * authentication flow. Routes the authenticated user to the correct
 * role-based view from {@link dev.saberlabs.CoffeeChatApp}.
 *
 * Only CUSTOMER self-registration is offered here. BARISTA accounts
 * are created exclusively by a MANAGER via {@link ManagerView}.
 */
public class LoginView {

    private static final String SEPARATOR =
            "════════════════════════════════════════════════════════";

    @NotNull private final AuthService authService;
    @NotNull private final Scanner scanner;

    public LoginView(@NotNull AuthService authService, @NotNull Scanner scanner) {
        this.authService = Objects.requireNonNull(authService, "AuthService cannot be null");
        this.scanner = Objects.requireNonNull(scanner, "Scanner cannot be null");
    }

    /**
     * Runs the login/register loop until a user successfully authenticates.
     *
     * @return the authenticated User
     */
    public @NotNull User run() {
        printBanner();

        while (true) {
            System.out.println("  1. Login");
            System.out.println("  2. Register");
            System.out.println("  0. Exit");
            System.out.print("\n  Your choice: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    User user = tryLogin();
                    if (user != null) return user;
                }
                case "2" -> {
                    User user = tryRegister();
                    if (user != null) return user;
                }
                case "0" -> {
                    System.out.println("\n  Goodbye!\n");
                    System.exit(0);
                }
                default -> System.out.println("  Invalid choice.\n");
            }
        }
    }

    private @org.jetbrains.annotations.Nullable User tryLogin() {
        System.out.print("\n  Username: ");
        String username = scanner.nextLine().trim();
        System.out.print("  Password: ");
        String password = scanner.nextLine().trim();

        try {
            User user = authService.login(username, password);
            System.out.printf("\n  ✓ Welcome back, %s (%s)!%n%n",
                    user.username(), user.role());
            return user;
        } catch (AuthException e) {
            System.out.println("\n  ✗ " + e.getMessage() + "\n");
            return null;
        }
    }

    private @org.jetbrains.annotations.Nullable User tryRegister() {
        System.out.print("\n  Choose a username (min 3 chars): ");
        String username = scanner.nextLine().trim();
        System.out.print("  Choose a password (min 6 chars): ");
        String password = scanner.nextLine().trim();

        try {
            User user = authService.register(username, password, Role.CUSTOMER);
            System.out.printf("\n  ✓ Account created! Welcome, %s.%n%n", user.username());
            return user;
        } catch (IllegalArgumentException e) {
            System.out.println("\n  ✗ " + e.getMessage() + "\n");
            return null;
        }
    }

    private void printBanner() {
        System.out.println();
        System.out.println(SEPARATOR);
        System.out.println("   ☕  COFFEE CHAT — LOGIN  ☕");
        System.out.println(SEPARATOR);
        System.out.println();
    }
}
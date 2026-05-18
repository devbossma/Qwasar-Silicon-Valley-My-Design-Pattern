package dev.saberlabs.command;

/**
 * Pattern 8: COMMAND (Command interface)
 *
 * Encapsulates an action as an object. Each command represents
 * an operation that can be executed (and potentially undone).
 */
public interface Command {

    /**
     * Execute the order command
     */
    void execute();


    /**
     * Undo the order command
     */
    void undo();

    /**
     *
     * @return the name of the command for logging or display purposes
     */
    String getCommandName();
}

package layerline;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** VIEW: reads lines and prints text. Holds no rules and no model reference. */
public class ConsoleView {

    private final Scanner input;

    public ConsoleView(Scanner input) {
        if (input == null) {
            throw new IllegalArgumentException("Scanner must not be null.");
        }
        this.input = input;
    }

    /** Prints the prompt and reads a whole line; returns null on end of input. */
    public String prompt(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        try {
            return input.nextLine();
        } catch (NoSuchElementException | IllegalStateException e) {
            return null;
        }
    }

    public void showMessage(String message) {
        System.out.println(message);
    }

    /** Prints 1-based places, names and minutes. Never prints codes. */
    public void showStatus(List<StatusEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            System.out.println("Line is empty");
            return;
        }
        System.out.println("Current line:");
        for (StatusEntry entry : entries) {
            System.out.println("  " + entry.getPosition() + ". " + entry.getName()
                    + " - " + entry.getEstimatedMinutes() + " min");
        }
    }
}

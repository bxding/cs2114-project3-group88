package layerline;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Display checks for ConsoleView (spec section 8). */
class ConsoleViewTest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream captured;

    @BeforeEach
    void captureOutput() {
        captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
    }

    private String printed() {
        return captured.toString(StandardCharsets.UTF_8);
    }

    @Test
    void nullScannerThrows() {
        assertThrows(IllegalArgumentException.class, () -> new ConsoleView(null));
    }

    @Test
    void promptPrintsTextAndReturnsLine() {
        ConsoleView view = new ConsoleView(new Scanner("hello world\n"));
        assertEquals("hello world", view.prompt("Name: "));
        assertTrue(printed().contains("Name: "));
    }

    @Test
    void promptReturnsNullAtEndOfInput() {
        ConsoleView view = new ConsoleView(new Scanner(""));
        assertNull(view.prompt("Name: "));
    }

    @Test
    void promptReturnsNullWhenScannerClosed() {
        Scanner scanner = new Scanner("join\n");
        scanner.close();
        assertNull(new ConsoleView(scanner).prompt("Command: "));
    }

    @Test
    void showStatusPrintsPlacesWithoutCodes() {
        LayerLineModel model = new LayerLineModel();
        String code = model.join("Sam", 30).getPersonalCode();
        model.join("Lee", 45);
        new ConsoleView(new Scanner("")).showStatus(model.getStatus());
        String out = printed();
        assertTrue(out.contains("1. Sam - 30 min"));
        assertTrue(out.contains("2. Lee - 45 min"));
        assertFalse(out.contains(code));
    }

    @Test
    void showStatusEmptyOrNullSaysLineIsEmpty() {
        ConsoleView view = new ConsoleView(new Scanner(""));
        view.showStatus(List.of());
        view.showStatus(null);
        assertEquals(2, printed().split("Line is empty", -1).length - 1);
    }
}

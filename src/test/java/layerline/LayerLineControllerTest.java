package layerline;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Controller + console integration tests (spec section 7.4). Each test feeds
 * scripted lines through a Scanner, captures System.out, and checks both the
 * printed text and the model state afterwards.
 */
class LayerLineControllerTest {

    private static final Pattern CODE = Pattern.compile("personal code is ([A-Z]{6})");

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

    /** Runs the controller on the given input lines; returns everything it printed. */
    private String run(LayerLineModel model, String... lines) {
        captured.reset();
        String input = String.join("\n", lines) + "\n";
        ConsoleView view = new ConsoleView(new Scanner(input));
        new LayerLineController(model, view).run();
        return captured.toString(StandardCharsets.UTF_8);
    }

    private static List<String> codesIn(String output) {
        Matcher m = CODE.matcher(output);
        List<String> codes = new java.util.ArrayList<>();
        while (m.find()) {
            codes.add(m.group(1));
        }
        return codes;
    }

    private static String names(LayerLineModel model) {
        StringBuilder sb = new StringBuilder();
        for (StatusEntry e : model.getStatus()) {
            sb.append(e.getPosition()).append(':').append(e.getName()).append(' ');
        }
        return sb.toString().trim();
    }

    @Test
    void constructorRejectsNulls() {
        ConsoleView view = new ConsoleView(new Scanner(""));
        assertThrows(IllegalArgumentException.class,
                () -> new LayerLineController(null, view));
        assertThrows(IllegalArgumentException.class,
                () -> new LayerLineController(new LayerLineModel(), null));
    }

    @Test
    void coreWorkflow() {
        LayerLineModel model = new LayerLineModel();
        String out = run(model, "join", "Sam", "30", "join", "Lee", "45",
                "status", "next", "quit");

        assertTrue(out.contains("Added at place 1"));
        assertTrue(out.contains("Added at place 2"));
        List<String> codes = codesIn(out);
        assertEquals(2, codes.size());
        assertNotEquals(codes.get(0), codes.get(1));
        assertTrue(out.contains("1. Sam - 30 min"));
        assertTrue(out.contains("2. Lee - 45 min"));
        assertTrue(out.contains("Now called: Sam (30 min)"));
        assertTrue(out.contains("Goodbye"));
        assertEquals("1:Lee", names(model));

        // Status lines never show a code; codes only appear in the "Added" message.
        for (String line : out.split("\n")) {
            if (line.contains(" min") && !line.contains("Added")) {
                assertFalse(line.contains(codes.get(0)));
                assertFalse(line.contains(codes.get(1)));
            }
        }

        String out2 = run(model, "cancel", codes.get(1), "quit");
        assertTrue(out2.contains("Request cancelled."));
        assertTrue(out2.contains("Line is empty"));
        assertTrue(model.getStatus().isEmpty());
    }

    @Test
    void badNumericStringsReprompt() {
        LayerLineModel model = new LayerLineModel();
        String out = run(model, "join", "Sam",
                "abc", "", "1.5", "99999999999", "-5", "0", "241", "30", "quit");

        int reprompts = out.split("Enter a whole number from 1 to 240", -1).length - 1;
        assertEquals(7, reprompts);
        assertEquals(1, model.getStatus().size());
        assertEquals(30, model.getStatus().get(0).getEstimatedMinutes());
    }

    @Test
    void duplicateCasingKeepsOneSpot() {
        LayerLineModel model = new LayerLineModel();
        String out = run(model, "join", "Sam", "30", "join", "sam ", "20",
                "join", "SAM", "60", "quit");

        int already = out.split("Already waiting at place 1", -1).length - 1;
        assertEquals(2, already);
        assertEquals(1, codesIn(out).size());
        assertEquals(1, model.getStatus().size());
        assertEquals(30, model.getStatus().get(0).getEstimatedMinutes());
    }

    @Test
    void wrongCodeAndEmptyNext() {
        LayerLineModel model = new LayerLineModel();
        String out = run(model, "cancel", "ZZZZZZZZZZ", "next", "quit");
        assertTrue(out.contains("Code not found"));
        assertTrue(out.contains("Line is empty"));
        assertTrue(model.getStatus().isEmpty());
    }

    @Test
    void queueFull() {
        LayerLineModel model = new LayerLineModel();
        String[] lines = new String[41 * 3 + 1];
        for (int i = 0; i < 41; i++) {
            lines[i * 3] = "join";
            lines[i * 3 + 1] = "Student" + (i + 1);
            lines[i * 3 + 2] = "10";
        }
        lines[lines.length - 1] = "quit";
        String out = run(model, lines);

        assertTrue(out.contains("Line full (40 maximum)."));
        List<StatusEntry> status = model.getStatus();
        assertEquals(40, status.size());
        assertEquals("Student1", status.get(0).getName());
        assertEquals("Student40", status.get(39).getName());
    }

    @Test
    void controlAndOversizeNames() {
        LayerLineModel model = new LayerLineModel();
        String out = run(model, "join", "A".repeat(21), "15", "Ka\u0007ri", "quit");

        assertTrue(out.contains("Name is too long"));
        assertEquals("1:Kari", names(model));
        assertEquals(15, model.getStatus().get(0).getEstimatedMinutes());
    }

    @Test
    void blankNameRepromptsName() {
        LayerLineModel model = new LayerLineModel();
        String out = run(model, "join", "   ", "15", "Bob", "quit");
        assertTrue(out.contains("Name cannot be blank."));
        assertEquals("1:Bob", names(model));
    }

    @Test
    void nameQuitIsJustData() {
        LayerLineModel model = new LayerLineModel();
        String out = run(model, "join", "quit", "5", "status", "quit");
        assertTrue(out.contains("1. quit - 5 min"));
        assertEquals("1:quit", names(model));
    }

    @Test
    void headMiddleTailCancel() {
        LayerLineModel model = new LayerLineModel();
        String out = run(model, "join", "A", "10", "join", "B", "10",
                "join", "C", "10", "join", "D", "10", "quit");
        List<String> codes = codesIn(out);
        assertEquals(4, codes.size());

        String out2 = run(model, "cancel", codes.get(0), "cancel", codes.get(2),
                "cancel", codes.get(3), "join", "E", "10", "status", "quit");
        int cancelled = out2.split("Request cancelled.", -1).length - 1;
        assertEquals(3, cancelled);
        assertEquals("1:B 2:E", names(model));
    }

    @Test
    void badCommandsListChoicesAndStatusIgnoresCase() {
        LayerLineModel model = new LayerLineModel();
        String out = run(model, "jion", "", "STATUS", "quit");
        int unknown = out.split("Valid choices: join, status, next, cancel, quit.", -1).length - 1;
        assertEquals(2, unknown);
        assertTrue(out.contains("Line is empty"));
        assertTrue(model.getStatus().isEmpty());
    }

    @Test
    void eofAtCommandPromptExitsCleanly() {
        LayerLineModel model = new LayerLineModel();
        captured.reset();
        ConsoleView view = new ConsoleView(new Scanner(""));
        assertDoesNotThrow(() -> new LayerLineController(model, view).run());
        assertTrue(model.getStatus().isEmpty());
    }

    @Test
    void eofDuringJoinAndCancelExitsCleanly() {
        LayerLineModel model = new LayerLineModel();
        assertDoesNotThrow(() -> run(model, "join"));
        assertDoesNotThrow(() -> run(model, "join", "Sam"));
        assertDoesNotThrow(() -> run(model, "join", "Sam", "abc"));
        assertDoesNotThrow(() -> run(model, "cancel"));
        assertTrue(model.getStatus().isEmpty());
    }
}

package layerline;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** WebController tests: raw form text in, message out. No HTTP server needed. */
class WebControllerTest {

    private static String codeIn(String message) {
        Matcher m = Pattern.compile("personal code is ([A-Z]{6})").matcher(message);
        assertTrue(m.find(), message);
        return m.group(1);
    }

    @Test
    void nullModelThrows() {
        assertThrows(IllegalArgumentException.class, () -> new WebController(null));
    }

    @Test
    void joinNextCancelAndStatus() {
        WebController c = new WebController(new LayerLineModel());
        String sam = codeIn(c.join("Sam", "30"));
        c.join("Lee", " 45 ");
        assertEquals(2, c.status().size());
        assertEquals("Now called: Sam (30 min).", c.next());
        String kim = codeIn(c.join("Kim", "10"));
        assertEquals("Request cancelled.", c.cancel(kim));
        assertEquals("Code not found.", c.cancel(sam)); // Sam is no longer waiting
        assertEquals(1, c.status().size());
    }

    @Test
    void joinBadInputNeverThrows() {
        WebController c = new WebController(new LayerLineModel());
        assertEquals("Enter a whole number from 1 to 240.", c.join("Sam", "abc"));
        assertEquals("Enter a whole number from 1 to 240.", c.join("Sam", null));
        assertEquals("Enter a whole number from 1 to 240.", c.join("Sam", "99999999999"));
        assertTrue(c.join("Sam", "0").startsWith("Minutes must be"));
        assertEquals("Name cannot be blank.", c.join("  ", "30"));
        assertEquals("Name must not be null.", c.join(null, "30"));
        assertTrue(c.status().isEmpty());
    }

    @Test
    void reprintThroughController() {
        WebController c = new WebController(new LayerLineModel());
        String sam = codeIn(c.join("Sam", "30"));
        c.join("Lee", "45");
        assertTrue(c.reprint(sam).startsWith("Reprint not allowed")); // not called yet
        c.next();
        assertEquals("Reprint accepted. You are now at place 1.", c.reprint(sam.toLowerCase()));
        assertEquals("Sam", c.status().get(0).getName());
        assertTrue(c.reprint(sam).startsWith("Reprint not allowed")); // used up
    }

    @Test
    void cancelAndReprintNullOrEmptyCode() {
        WebController c = new WebController(new LayerLineModel());
        assertEquals("Code not found.", c.cancel(null));
        assertEquals("Code not found.", c.cancel(""));
        assertTrue(c.reprint(null).startsWith("Reprint not allowed"));
        assertEquals("Line is empty.", c.next());
    }

    @Test
    void reprintRefusalSaysWhy() {
        LayerLineModel model = new LayerLineModel();
        WebController c = new WebController(model);
        String sam = codeIn(c.join("Sam", "30"));
        assertEquals("Reprint not allowed: that code is still waiting in line.", c.reprint(sam));
        c.next();
        for (int i = 0; i < LayerLineModel.MAX_WAITING; i++) {
            c.join("P" + i, "10");
        }
        assertTrue(c.reprint(sam).startsWith("Reprint not allowed: the line is full"));
        assertTrue(c.reprint("ZZZZZZ").startsWith("Reprint not allowed: code not found."));
    }
}

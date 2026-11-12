package layerline;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class LayerLineModelTest {

    @Test
    void newModelIsEmpty() {
        LayerLineModel m = new LayerLineModel();
        assertTrue(m.getStatus().isEmpty());
        assertNull(m.callNext());
    }

    @Test
    void modelsAreIndependent() {
        LayerLineModel a = new LayerLineModel();
        LayerLineModel b = new LayerLineModel();
        a.join("Sam", 30);
        assertTrue(b.getStatus().isEmpty());
    }

    @Test
    void joinAddsWithSixLetterCode() {
        LayerLineModel m = new LayerLineModel();
        JoinResult r = m.join("Sam", 30);
        assertEquals(JoinOutcome.ADDED, r.getOutcome());
        assertEquals(1, r.getPosition());
        assertTrue(r.getPersonalCode().matches("[A-Z]{6}"));
        List<StatusEntry> s = m.getStatus();
        assertEquals(1, s.size());
        assertEquals("Sam", s.get(0).getName());
        assertEquals(30, s.get(0).getEstimatedMinutes());
    }

    @Test
    void joinRejectsBadInput() {
        LayerLineModel m = new LayerLineModel();
        assertThrows(IllegalArgumentException.class, () -> m.join("", 30));
        assertThrows(IllegalArgumentException.class, () -> m.join("   ", 30));
        assertThrows(IllegalArgumentException.class, () -> m.join(null, 30));
        assertThrows(IllegalArgumentException.class, () -> m.join("\t\n", 30));
        assertThrows(IllegalArgumentException.class, () -> m.join("Sam", 0));
        assertThrows(IllegalArgumentException.class, () -> m.join("Sam", 241));
        assertThrows(IllegalArgumentException.class, () -> m.join("A".repeat(21), 30));
        assertTrue(m.getStatus().isEmpty());
    }

    @Test
    void joinStripsControlCharactersAndTrims() {
        LayerLineModel m = new LayerLineModel();
        m.join(" S\tam ", 30);
        assertEquals("Sam", m.getStatus().get(0).getName());
    }

    @Test
    void boundaryValuesAccepted() {
        LayerLineModel m = new LayerLineModel();
        m.join("One", 1);
        m.join("A".repeat(20), 240);
        List<StatusEntry> s = m.getStatus();
        assertEquals(1, s.get(0).getEstimatedMinutes());
        assertEquals(240, s.get(1).getEstimatedMinutes());
    }

    @Test
    void duplicateNameReturnsAlreadyWaiting() {
        LayerLineModel m = new LayerLineModel();
        m.join("Sam", 30);
        JoinResult r = m.join("sam ", 30);
        assertEquals(JoinOutcome.ALREADY_WAITING, r.getOutcome());
        assertEquals(1, r.getPosition());
        assertNull(r.getPersonalCode());
        assertEquals(1, m.getStatus().size());
        assertEquals(JoinOutcome.ALREADY_WAITING, m.join("SAM", 60).getOutcome());
        assertEquals(30, m.getStatus().get(0).getEstimatedMinutes());
    }

    @Test
    void fortyFirstDistinctJoinIsFull() {
        LayerLineModel m = new LayerLineModel();
        for (int i = 1; i <= 40; i++) {
            assertEquals(JoinOutcome.ADDED, m.join("Student" + i, 10).getOutcome());
        }
        JoinResult r = m.join("Extra", 10);
        assertEquals(JoinOutcome.FULL, r.getOutcome());
        assertEquals(-1, r.getPosition());
        assertNull(r.getPersonalCode());
        assertEquals(40, m.getStatus().size());
    }

    @Test
    void duplicateStillSeesPlaceWhenFull() {
        LayerLineModel m = new LayerLineModel();
        for (int i = 1; i <= 40; i++) {
            m.join("Student" + i, 10);
        }
        JoinResult r = m.join("student7", 10);
        assertEquals(JoinOutcome.ALREADY_WAITING, r.getOutcome());
        assertEquals(7, r.getPosition());
    }

    @Test
    void callNextIsFifo() {
        LayerLineModel m = new LayerLineModel();
        m.join("Sam", 30);
        m.join("Lee", 45);
        StatusEntry e = m.callNext();
        assertEquals("Sam", e.getName());
        assertEquals(30, e.getEstimatedMinutes());
        assertEquals(1, e.getPosition());
        List<StatusEntry> s = m.getStatus();
        assertEquals(1, s.size());
        assertEquals("Lee", s.get(0).getName());
    }

    @Test
    void callNextOnEmptyReturnsNull() {
        LayerLineModel m = new LayerLineModel();
        assertNull(m.callNext());
        assertTrue(m.getStatus().isEmpty());
    }

    @Test
    void cancelMiddleKeepsOrder() {
        LayerLineModel m = new LayerLineModel();
        m.join("A", 10);
        String codeB = m.join("B", 10).getPersonalCode();
        m.join("C", 10);
        assertTrue(m.cancel(codeB));
        List<StatusEntry> s = m.getStatus();
        assertEquals(2, s.size());
        assertEquals("A", s.get(0).getName());
        assertEquals(1, s.get(0).getPosition());
        assertEquals("C", s.get(1).getName());
        assertEquals(2, s.get(1).getPosition());
    }

    @Test
    void cancelAcceptsLowerCaseAndSpaces() {
        LayerLineModel m = new LayerLineModel();
        String code = m.join("A", 10).getPersonalCode();
        assertTrue(m.cancel("  " + code.toLowerCase() + " "));
        assertTrue(m.getStatus().isEmpty());
    }

    @Test
    void cancelBadCodes() {
        LayerLineModel m = new LayerLineModel();
        m.join("A", 10);
        assertFalse(m.cancel("ZZZZZZZZZZ"));
        assertFalse(m.cancel(""));
        assertFalse(m.cancel("   "));
        assertThrows(IllegalArgumentException.class, () -> m.cancel(null));
        assertEquals(1, m.getStatus().size());
    }

    @Test
    void cancelHeadMiddleTailThenJoin() {
        LayerLineModel m = new LayerLineModel();
        String a = m.join("A", 10).getPersonalCode();
        m.join("B", 10);
        String c = m.join("C", 10).getPersonalCode();
        String d = m.join("D", 10).getPersonalCode();
        assertTrue(m.cancel(a));
        assertTrue(m.cancel(c));
        assertTrue(m.cancel(d));
        m.join("E", 10);
        List<StatusEntry> s = m.getStatus();
        assertEquals(2, s.size());
        assertEquals("B", s.get(0).getName());
        assertEquals("E", s.get(1).getName());
        assertEquals(2, s.get(1).getPosition());
    }

    @Test
    void getStatusIsACopyWithoutCodes() {
        LayerLineModel m = new LayerLineModel();
        assertTrue(m.getStatus().isEmpty());
        m.join("A", 10);
        m.join("B", 20);
        List<StatusEntry> s = m.getStatus();
        assertEquals(1, s.get(0).getPosition());
        assertEquals(2, s.get(1).getPosition());
        s.clear();
        assertEquals(2, m.getStatus().size());
    }

    @Test
    void quitIsJustAName() {
        LayerLineModel m = new LayerLineModel();
        m.join("quit", 5);
        assertEquals("quit", m.getStatus().get(0).getName());
    }

    @Test
    void duplicateNeverRevealsOriginalCode() {
        LayerLineModel m = new LayerLineModel();
        String code = m.join("Sam", 30).getPersonalCode();
        JoinResult dup = m.join("Sam", 30);
        assertNull(dup.getPersonalCode());
        assertNotNull(code);
    }
}

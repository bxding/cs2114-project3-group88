package layerline;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class LinkedQueueTest {

    @Test
    void newQueueIsEmpty() {
        LinkedQueue<String> q = new LinkedQueue<>();
        assertEquals(0, q.size());
        assertTrue(q.isEmpty());
        assertNull(q.peek());
    }

    @Test
    void queuesDoNotShareState() {
        LinkedQueue<String> a = new LinkedQueue<>();
        a.enqueue("A");
        LinkedQueue<String> b = new LinkedQueue<>();
        assertTrue(b.isEmpty());
    }

    @Test
    void enqueueKeepsOrder() {
        LinkedQueue<String> q = new LinkedQueue<>();
        q.enqueue("A");
        q.enqueue("B");
        assertEquals(2, q.size());
        assertEquals(List.of("A", "B"), q.snapshot());
    }

    @Test
    void enqueueNullThrowsAndLeavesQueueUnchanged() {
        LinkedQueue<String> q = new LinkedQueue<>();
        q.enqueue("A");
        assertThrows(IllegalArgumentException.class, () -> q.enqueue(null));
        assertEquals(1, q.size());
        assertEquals(List.of("A"), q.snapshot());
    }

    @Test
    void dequeueReturnsHead() {
        LinkedQueue<String> q = new LinkedQueue<>();
        q.enqueue("A");
        q.enqueue("B");
        assertEquals("A", q.dequeue());
        assertEquals(List.of("B"), q.snapshot());
    }

    @Test
    void dequeueEmptyReturnsNull() {
        LinkedQueue<String> q = new LinkedQueue<>();
        assertNull(q.dequeue());
        assertEquals(0, q.size());
    }

    @Test
    void dequeueLastResetsTail() {
        LinkedQueue<String> q = new LinkedQueue<>();
        q.enqueue("A");
        assertEquals("A", q.dequeue());
        q.enqueue("B");
        assertEquals(List.of("B"), q.snapshot());
        assertEquals(1, q.size());
    }

    @Test
    void peekDoesNotRemove() {
        LinkedQueue<String> q = new LinkedQueue<>();
        q.enqueue("A");
        q.enqueue("B");
        assertEquals("A", q.peek());
        assertEquals(2, q.size());
    }

    @Test
    void isEmptyTracksState() {
        LinkedQueue<String> q = new LinkedQueue<>();
        assertTrue(q.isEmpty());
        q.enqueue("A");
        assertFalse(q.isEmpty());
        q.dequeue();
        assertTrue(q.isEmpty());
    }

    @Test
    void sizeNeverNegative() {
        LinkedQueue<String> q = new LinkedQueue<>();
        q.enqueue("A");
        q.enqueue("B");
        q.enqueue("C");
        assertEquals(3, q.size());
        q.dequeue();
        q.dequeue();
        q.dequeue();
        q.dequeue();
        assertEquals(0, q.size());
    }

    @Test
    void removeFirstMiddle() {
        LinkedQueue<String> q = new LinkedQueue<>();
        q.enqueue("A");
        q.enqueue("B");
        q.enqueue("C");
        assertEquals("B", q.removeFirst("B"));
        assertEquals(List.of("A", "C"), q.snapshot());
    }

    @Test
    void removeFirstHead() {
        LinkedQueue<String> q = new LinkedQueue<>();
        q.enqueue("A");
        q.enqueue("B");
        assertEquals("A", q.removeFirst("A"));
        assertEquals(List.of("B"), q.snapshot());
    }

    @Test
    void removeFirstTailThenEnqueue() {
        LinkedQueue<String> q = new LinkedQueue<>();
        q.enqueue("A");
        q.enqueue("B");
        q.enqueue("C");
        assertEquals("C", q.removeFirst("C"));
        q.enqueue("D");
        assertEquals(List.of("A", "B", "D"), q.snapshot());
    }

    @Test
    void removeFirstOnlyElementThenEnqueue() {
        LinkedQueue<String> q = new LinkedQueue<>();
        q.enqueue("A");
        assertEquals("A", q.removeFirst("A"));
        assertTrue(q.isEmpty());
        q.enqueue("B");
        assertEquals(List.of("B"), q.snapshot());
    }

    @Test
    void removeFirstNoMatchAndNullTarget() {
        LinkedQueue<String> q = new LinkedQueue<>();
        q.enqueue("A");
        assertNull(q.removeFirst("Z"));
        assertEquals(1, q.size());
        assertThrows(IllegalArgumentException.class, () -> q.removeFirst(null));
        assertEquals(List.of("A"), q.snapshot());
    }

    @Test
    void snapshotIsACopy() {
        LinkedQueue<String> q = new LinkedQueue<>();
        assertTrue(q.snapshot().isEmpty());
        q.enqueue("A");
        q.enqueue("B");
        List<String> copy = q.snapshot();
        assertEquals(List.of("A", "B"), copy);
        copy.add("C");
        assertEquals(2, q.size());
    }
}

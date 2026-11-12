package layerline;

import java.util.ArrayList;
import java.util.List;

/**
 * MODEL: singly linked FIFO queue. enqueue/dequeue are O(1); removeFirst and
 * snapshot are O(n). Node is private and never leaves this class.
 */
public class LinkedQueue<T> {

    private static class Node<T> {
        private final T data;
        private Node<T> next;

        private Node(T data) {
            this.data = data;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    public LinkedQueue() {
        head = null;
        tail = null;
        size = 0;
    }

    /** Appends a non-null value at the tail. */
    public void enqueue(T value) {
        if (value == null) {
            throw new IllegalArgumentException("Queue does not accept null entries.");
        }
        Node<T> node = new Node<>(value);
        if (tail == null) {
            head = node;
        } else {
            tail.next = node;
        }
        tail = node;
        size++;
    }

    /** Removes and returns the head, or null if empty. */
    public T dequeue() {
        if (head == null) {
            return null;
        }
        T value = head.data;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        size--;
        return value;
    }

    /** Returns the head without removing it, or null if empty. */
    public T peek() {
        return head == null ? null : head.data;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    /**
     * Removes the first node whose data equals target and returns it.
     * Returns null if there is no match. Fixes head/tail for head, middle
     * and tail removals.
     */
    public T removeFirst(T target) {
        if (target == null) {
            throw new IllegalArgumentException("Target must not be null.");
        }
        Node<T> prev = null;
        Node<T> cur = head;
        while (cur != null) {
            if (target.equals(cur.data)) {
                if (prev == null) {
                    head = cur.next;
                } else {
                    prev.next = cur.next;
                }
                if (cur == tail) {
                    tail = prev;
                }
                cur.next = null;
                size--;
                return cur.data;
            }
            prev = cur;
            cur = cur.next;
        }
        return null;
    }

    /** Returns a new list copy in FIFO order; changing it never changes the queue. */
    public List<T> snapshot() {
        List<T> copy = new ArrayList<>(size);
        for (Node<T> cur = head; cur != null; cur = cur.next) {
            copy.add(cur.data);
        }
        return copy;
    }
}

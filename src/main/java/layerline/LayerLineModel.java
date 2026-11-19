package layerline;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** MODEL: owns the line and enforces every rule. No console I/O. */
public class LayerLineModel {

    static final int MAX_WAITING = 40;
    static final int MIN_MINUTES = 1;
    static final int MAX_MINUTES = 240;
    static final int MAX_NAME_LENGTH = 20;
    static final int CODE_LENGTH = 6;

    private final LinkedQueue<PrintRequest> line;
    /** Called with next and still allowed one reprint. */
    private final LinkedQueue<PrintRequest> served;
    /** Back in line after a reprint; not added to served again when called. */
    private final LinkedQueue<PrintRequest> reprinted;
    private final Random random;

    public LayerLineModel() {
        this.line = new LinkedQueue<>();
        this.served = new LinkedQueue<>();
        this.reprinted = new LinkedQueue<>();
        this.random = new Random();
    }

    /**
     * Precedence: validate name and minutes, then duplicate check, then cap.
     *
     * @throws IllegalArgumentException for null/blank/overlong name or bad minutes
     */
    public JoinResult join(String name, int minutes) {
        String cleaned = cleanName(name);
        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be blank.");
        }
        if (cleaned.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "Name is too long (maximum " + MAX_NAME_LENGTH + " characters).");
        }
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
            throw new IllegalArgumentException(
                    "Minutes must be a whole number from " + MIN_MINUTES + " to " + MAX_MINUTES + ".");
        }

        String normalized = cleaned.toLowerCase();
        List<PrintRequest> current = line.snapshot();
        for (int i = 0; i < current.size(); i++) {
            if (current.get(i).getNormalizedName().equals(normalized)) {
                return new JoinResult(JoinOutcome.ALREADY_WAITING, i + 1, null);
            }
        }

        if (line.size() >= MAX_WAITING) {
            return new JoinResult(JoinOutcome.FULL, -1, null);
        }

        String code = makeCode();
        line.enqueue(new PrintRequest(cleaned, minutes, code));
        return new JoinResult(JoinOutcome.ADDED, line.size(), code);
    }

    /** Removes the FIFO head; returns its name/minutes with former position 1, or null if empty. */
    public StatusEntry callNext() {
        PrintRequest head = line.dequeue();
        if (head == null) {
            return null;
        }
        if (reprinted.removeFirst(head) == null) {
            served.enqueue(head); // first time called: one reprint allowed
        }
        return new StatusEntry(1, head.getDisplayName(), head.getMinutes());
    }

    /**
     * Puts a student who was already called back at place 1, once per code.
     *
     * @return true if they are now first; false for a blank or unknown code, a
     *         code still waiting or already reprinted, a full line, or a name
     *         that is already waiting
     * @throws IllegalArgumentException if code is null
     */
    public boolean reprint(String personalCode) {
        if (personalCode == null) {
            throw new IllegalArgumentException("Code must not be null.");
        }
        String code = personalCode.trim().toUpperCase();
        if (code.isEmpty()) {
            return false;
        }
        PrintRequest request = findByCode(served, code);
        if (request == null || line.size() >= MAX_WAITING
                || findByName(request.getNormalizedName()) != null) {
            return false;
        }
        served.removeFirst(request);
        reprinted.enqueue(request);
        line.enqueueFront(request);
        return true;
    }

    /**
     * Cancels the request with the given code.
     *
     * @return true if removed; false for unknown/blank code
     * @throws IllegalArgumentException if code is null
     */
    public boolean cancel(String personalCode) {
        if (personalCode == null) {
            throw new IllegalArgumentException("Code must not be null.");
        }
        String code = personalCode.trim().toUpperCase();
        if (code.isEmpty()) {
            return false;
        }
        PrintRequest request = findByCode(line, code);
        if (request == null) {
            return false;
        }
        line.removeFirst(request);
        reprinted.removeFirst(request);
        return true;
    }

    /** New list in line order, places starting at 1, no codes. */
    public List<StatusEntry> getStatus() {
        List<StatusEntry> entries = new ArrayList<>();
        int place = 1;
        for (PrintRequest request : line.snapshot()) {
            entries.add(new StatusEntry(place++, request.getDisplayName(), request.getMinutes()));
        }
        return entries;
    }

    private static String cleanName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name must not be null.");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isISOControl(c)) {
                sb.append(c);
            }
        }
        return sb.toString().trim();
    }

    /** Random code not used by anyone waiting or anyone who can still reprint. */
    private String makeCode() {
        while (true) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append((char) ('A' + random.nextInt(26)));
            }
            String candidate = sb.toString();
            if (findByCode(line, candidate) == null && findByCode(served, candidate) == null) {
                return candidate;
            }
        }
    }

    private static PrintRequest findByCode(LinkedQueue<PrintRequest> queue, String code) {
        for (PrintRequest request : queue.snapshot()) {
            if (request.getPersonalCode().equals(code)) {
                return request;
            }
        }
        return null;
    }

    private PrintRequest findByName(String normalizedName) {
        for (PrintRequest request : line.snapshot()) {
            if (request.getNormalizedName().equals(normalizedName)) {
                return request;
            }
        }
        return null;
    }
}

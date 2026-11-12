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
    private final Random random;

    public LayerLineModel() {
        this.line = new LinkedQueue<>();
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

        String code = makeCode(current);
        line.enqueue(new PrintRequest(cleaned, minutes, code));
        return new JoinResult(JoinOutcome.ADDED, line.size(), code);
    }

    /** Removes the FIFO head; returns its name/minutes with former position 1, or null if empty. */
    public StatusEntry callNext() {
        PrintRequest head = line.dequeue();
        if (head == null) {
            return null;
        }
        return new StatusEntry(1, head.getDisplayName(), head.getMinutes());
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
        for (PrintRequest request : line.snapshot()) {
            if (request.getPersonalCode().equals(code)) {
                line.removeFirst(request);
                return true;
            }
        }
        return false;
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

    private String makeCode(List<PrintRequest> current) {
        while (true) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append((char) ('A' + random.nextInt(26)));
            }
            String candidate = sb.toString();
            boolean taken = false;
            for (PrintRequest request : current) {
                if (request.getPersonalCode().equals(candidate)) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                return candidate;
            }
        }
    }
}

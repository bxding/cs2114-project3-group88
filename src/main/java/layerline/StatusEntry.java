package layerline;

/** MODEL: read-only row of the status list. Never holds a personal code. */
public final class StatusEntry {
    private final int position;
    private final String name;
    private final int estimatedMinutes;

    StatusEntry(int position, String name, int estimatedMinutes) {
        this.position = position;
        this.name = name;
        this.estimatedMinutes = estimatedMinutes;
    }

    public int getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }
}

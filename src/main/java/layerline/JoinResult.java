package layerline;

/** MODEL: read-only result of a join. Code is non-null only when ADDED. */
public final class JoinResult {
    private final JoinOutcome outcome;
    private final int position;
    private final String personalCode;

    JoinResult(JoinOutcome outcome, int position, String personalCode) {
        this.outcome = outcome;
        this.position = position;
        this.personalCode = personalCode;
    }

    public JoinOutcome getOutcome() {
        return outcome;
    }

    /** One-based place for ADDED / ALREADY_WAITING; -1 for FULL. */
    public int getPosition() {
        return position;
    }

    /** New 6-letter code only when ADDED; otherwise null. */
    public String getPersonalCode() {
        return personalCode;
    }
}

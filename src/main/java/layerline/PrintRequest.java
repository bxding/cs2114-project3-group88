package layerline;

/** MODEL (package-private): one waiting student's immutable request. */
final class PrintRequest {
    private final String displayName;
    private final String normalizedName;
    private final int minutes;
    private final String personalCode;

    PrintRequest(String displayName, int minutes, String personalCode) {
        this.displayName = displayName;
        this.normalizedName = displayName.toLowerCase();
        this.minutes = minutes;
        this.personalCode = personalCode;
    }

    String getDisplayName() {
        return displayName;
    }

    String getNormalizedName() {
        return normalizedName;
    }

    int getMinutes() {
        return minutes;
    }

    String getPersonalCode() {
        return personalCode;
    }
}

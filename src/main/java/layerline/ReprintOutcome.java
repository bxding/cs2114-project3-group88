package layerline;

/** MODEL: labels the result of a reprint attempt, so controllers can say why it failed. */
public enum ReprintOutcome {
    /** Back in line at place 1. */
    ACCEPTED,
    /** Blank or unknown code, reprint already used, or called too long ago. */
    NOT_FOUND,
    /** The code belongs to someone who is still waiting in line. */
    STILL_WAITING,
    /** The line already has MAX_WAITING people. */
    LINE_FULL,
    /** Someone with the same name is already waiting. */
    NAME_WAITING
}

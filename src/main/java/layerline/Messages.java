package layerline;

/**
 * CONTROLLER: every user-facing sentence, shared by LayerLineController and
 * WebController so each message is written once. Turns model results into text.
 */
final class Messages {

    static final String BAD_MINUTES = "Enter a whole number from " + LayerLineModel.MIN_MINUTES
            + " to " + LayerLineModel.MAX_MINUTES + ".";
    static final String LINE_EMPTY = "Line is empty.";
    static final String CANCELLED = "Request cancelled.";
    static final String CODE_NOT_FOUND = "Code not found.";

    private Messages() {
    }

    static String join(JoinResult result) {
        switch (result.getOutcome()) {
            case ADDED:
                return "Added at place " + result.getPosition() + ". Your personal code is "
                        + result.getPersonalCode()
                        + ". Save it; it is shown only once and is needed to cancel or reprint.";
            case ALREADY_WAITING:
                return "Already waiting at place " + result.getPosition() + ".";
            case FULL:
                return "Line full (" + LayerLineModel.MAX_WAITING + " maximum).";
            default:
                return "";
        }
    }

    static String called(StatusEntry called) {
        return "Now called: " + called.getName() + " (" + called.getEstimatedMinutes() + " min).";
    }

    static String reprint(ReprintOutcome outcome) {
        switch (outcome) {
            case ACCEPTED:
                return "Reprint accepted. You are now at place 1.";
            case STILL_WAITING:
                return "Reprint not allowed: that code is still waiting in line.";
            case LINE_FULL:
                return "Reprint not allowed: the line is full (" + LayerLineModel.MAX_WAITING
                        + " maximum). Try again after the next call.";
            case NAME_WAITING:
                return "Reprint not allowed: someone with that name is already waiting.";
            case NOT_FOUND:
            default:
                return "Reprint not allowed: code not found. Each code can reprint once, "
                        + "and only within the last " + LayerLineModel.REPRINT_WINDOW
                        + " students called.";
        }
    }
}

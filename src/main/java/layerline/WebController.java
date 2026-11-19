package layerline;

import java.util.List;

/**
 * CONTROLLER for the web view: takes raw form text, calls the model, and
 * returns the message to show. The web view never holds the model.
 */
public class WebController {

    private final LayerLineModel model;

    public WebController(LayerLineModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Model must not be null.");
        }
        this.model = model;
    }

    /** Joins using raw form text; never throws for bad input. */
    public String join(String name, String minutesText) {
        int minutes;
        try {
            minutes = Integer.parseInt(minutesText == null ? "" : minutesText.trim());
        } catch (NumberFormatException e) {
            return "Enter a whole number from 1 to 240.";
        }
        try {
            JoinResult result = model.join(name, minutes);
            switch (result.getOutcome()) {
                case ADDED:
                    return "Added at place " + result.getPosition() + ". Your personal code is "
                            + result.getPersonalCode()
                            + ". Save it; it is shown only once and is needed to cancel or reprint.";
                case ALREADY_WAITING:
                    return "Already waiting at place " + result.getPosition() + ".";
                case FULL:
                    return "Line full (40 maximum).";
                default:
                    return "";
            }
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    public String next() {
        StatusEntry called = model.callNext();
        if (called == null) {
            return "Line is empty.";
        }
        return "Now called: " + called.getName() + " (" + called.getEstimatedMinutes() + " min).";
    }

    public String cancel(String code) {
        return model.cancel(code == null ? "" : code) ? "Request cancelled." : "Code not found.";
    }

    public String reprint(String code) {
        if (model.reprint(code == null ? "" : code)) {
            return "Reprint accepted. You are now at place 1.";
        }
        return "Reprint not allowed. Only a student who was already called can reprint, "
                + "once per code, and only if the line is not full.";
    }

    /** Fresh copy of the line for the page; holds no codes. */
    public List<StatusEntry> status() {
        return model.getStatus();
    }
}

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
            return Messages.BAD_MINUTES;
        }
        try {
            return Messages.join(model.join(name, minutes));
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    public String next() {
        StatusEntry called = model.callNext();
        if (called == null) {
            return Messages.LINE_EMPTY;
        }
        return Messages.called(called);
    }

    public String cancel(String code) {
        return model.cancel(code == null ? "" : code) ? Messages.CANCELLED : Messages.CODE_NOT_FOUND;
    }

    public String reprint(String code) {
        return Messages.reprint(model.reprint(code == null ? "" : code));
    }

    /** Fresh copy of the line for the page; holds no codes. */
    public List<StatusEntry> status() {
        return model.getStatus();
    }
}

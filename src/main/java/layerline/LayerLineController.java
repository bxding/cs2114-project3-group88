package layerline;

/** CONTROLLER: command loop; checks typed input shape, calls model, tells view what to show. */
public class LayerLineController {

    private final LayerLineModel model;
    private final ConsoleView view;

    public LayerLineController(LayerLineModel model, ConsoleView view) {
        if (model == null || view == null) {
            throw new IllegalArgumentException("Model and view must not be null.");
        }
        this.model = model;
        this.view = view;
    }

    public void run() {
        view.showMessage("Layer Line - 3D print queue");
        while (true) {
            String raw = view.prompt("Command (join/status/next/cancel/quit): ");
            if (raw == null) {
                return; // EOF
            }
            String command = raw.trim().toLowerCase();
            switch (command) {
                case "join":
                    handleJoin();
                    break;
                case "status":
                    handleStatus();
                    break;
                case "next":
                    handleNext();
                    break;
                case "cancel":
                    handleCancel();
                    break;
                case "quit":
                    view.showMessage("Goodbye. Pending requests are not saved.");
                    return;
                default:
                    view.showMessage("Unknown command. Valid choices: join, status, next, cancel, quit.");
            }
        }
    }

    private void handleJoin() {
        int minutes = -1;
        boolean haveMinutes = false;
        while (true) {
            String name = view.prompt("Name (max 20 characters): ");
            if (name == null) {
                return;
            }
            if (!haveMinutes) {
                minutes = readMinutes();
                if (minutes == -1) {
                    return;
                }
                haveMinutes = true;
            }
            try {
                JoinResult result = model.join(name, minutes);
                switch (result.getOutcome()) {
                    case ADDED:
                        view.showMessage("Added at place " + result.getPosition()
                                + ". Your personal code is " + result.getPersonalCode()
                                + " - save it, it is shown only once and is needed to cancel.");
                        refreshStatus();
                        break;
                    case ALREADY_WAITING:
                        view.showMessage("Already waiting at place " + result.getPosition() + ".");
                        break;
                    case FULL:
                        view.showMessage("Line full (40 maximum).");
                        break;
                    default:
                        break;
                }
                return;
            } catch (IllegalArgumentException e) {
                view.showMessage(e.getMessage());
            }
        }
    }

    private int readMinutes() {
        while (true) {
            String text = view.prompt("Estimated print minutes (1-240): ");
            if (text == null) {
                return -1;
            }
            try {
                int value = Integer.parseInt(text.trim());
                if (value >= 1 && value <= 240) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // fall through to message
            }
            view.showMessage("Enter a whole number from 1 to 240");
        }
    }

    private void handleStatus() {
        refreshStatus();
    }

    private void handleNext() {
        StatusEntry called = model.callNext();
        if (called == null) {
            view.showMessage("Line is empty");
            return;
        }
        view.showMessage("Now called: " + called.getName() + " (" + called.getEstimatedMinutes() + " min)");
        refreshStatus();
    }

    private void handleCancel() {
        String code = view.prompt("Personal code: ");
        if (code == null) {
            return;
        }
        if (model.cancel(code)) {
            view.showMessage("Request cancelled.");
            refreshStatus();
        } else {
            view.showMessage("Code not found");
        }
    }

    private void refreshStatus() {
        view.showStatus(model.getStatus());
    }
}

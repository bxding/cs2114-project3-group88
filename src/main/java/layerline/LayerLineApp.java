package layerline;

import java.util.Scanner;

/** CONTROLLER (startup): wires model, view and controller, then runs. */
public class LayerLineApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ConsoleView view = new ConsoleView(scanner);
        LayerLineModel model = new LayerLineModel();
        LayerLineController controller = new LayerLineController(model, view);
        controller.run();
    }
}

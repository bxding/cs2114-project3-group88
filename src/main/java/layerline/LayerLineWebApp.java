package layerline;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Local web interface backed by a single in-memory queue. */
public final class LayerLineWebApp {
    private final LayerLineModel model = new LayerLineModel();

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        LayerLineWebApp app = new LayerLineWebApp();
        server.createContext("/", app::handle);
        server.start();
        System.out.println("Layer Line listening on port " + port);
    }

    private synchronized void handle(HttpExchange exchange) throws IOException {
        try {
            if (!exchange.getRequestURI().getPath().equals("/")) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            String message = "";
            if (exchange.getRequestMethod().equals("POST")) {
                byte[] body = exchange.getRequestBody().readNBytes(4097);
                if (body.length > 4096) {
                    exchange.sendResponseHeaders(413, -1);
                    return;
                }
                Map<String, String> form = parseForm(new String(body, StandardCharsets.UTF_8));
                try {
                    switch (form.getOrDefault("action", "")) {
                        case "join":
                            JoinResult result = model.join(form.get("name"), Integer.parseInt(form.getOrDefault("minutes", "")));
                            switch (result.getOutcome()) {
                                case ADDED: message = "Added at place " + result.getPosition() + ". Your personal code is " + result.getPersonalCode() + ". Save it; it is shown only once and is needed to cancel."; break;
                                case ALREADY_WAITING: message = "Already waiting at place " + result.getPosition() + "."; break;
                                case FULL: message = "Line full (40 maximum)."; break;
                                default: break;
                            }
                            break;
                        case "cancel": message = model.cancel(form.getOrDefault("code", "")) ? "Request cancelled." : "Code not found."; break;
                        case "next":
                            StatusEntry next = model.callNext();
                            message = next == null ? "Line is empty." : "Now called: " + next.getName() + " (" + next.getEstimatedMinutes() + " min).";
                            break;
                        default: message = "Unknown action.";
                    }
                } catch (NumberFormatException e) {
                    message = "Enter a whole number from 1 to 240.";
                } catch (IllegalArgumentException e) {
                    message = e.getMessage();
                }
            } else if (!exchange.getRequestMethod().equals("GET")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            byte[] response = page(message).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
        } finally {
            exchange.close();
        }
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> values = new HashMap<>();
        for (String part : body.split("&")) {
            String[] pair = part.split("=", 2);
            values.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), URLDecoder.decode(pair.length > 1 ? pair[1] : "", StandardCharsets.UTF_8));
        }
        return values;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private String page(String message) {
        StringBuilder rows = new StringBuilder();
        for (StatusEntry entry : model.getStatus()) {
            rows.append("<tr><td>").append(entry.getPosition()).append("</td><td>").append(escape(entry.getName())).append("</td><td>").append(entry.getEstimatedMinutes()).append("</td></tr>");
        }
        if (rows.length() == 0) rows.append("<tr><td colspan=\"3\" class=\"text-muted\">The queue is empty.</td></tr>");
        return "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>Layer Line</title><link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css\" rel=\"stylesheet\"></head><body class=\"bg-light\"><main class=\"container py-5\" style=\"max-width: 800px\"><h1>Layer Line</h1><p class=\"text-secondary\">Group project by Brian Ding, Ajeet Bondugula, Karthik Mittadhoddi</p>"
            + (message.isEmpty() ? "" : "<div class=\"alert alert-info\" role=\"status\">" + escape(message) + "</div>")
            + "<section class=\"card mb-4\"><div class=\"card-body\"><h2 class=\"h4\">Join the queue</h2><form method=\"post\" class=\"row g-3\"><input type=\"hidden\" name=\"action\" value=\"join\"><div class=\"col-sm-6\"><label class=\"form-label\" for=\"name\">Name</label><input class=\"form-control\" id=\"name\" name=\"name\" maxlength=\"20\" required></div><div class=\"col-sm-6\"><label class=\"form-label\" for=\"minutes\">Estimated minutes</label><input class=\"form-control\" id=\"minutes\" name=\"minutes\" type=\"number\" min=\"1\" max=\"240\" required></div><div><button class=\"btn btn-primary\">Join queue</button></div></form></div></section>"
            + "<section class=\"card mb-4\"><div class=\"card-body\"><h2 class=\"h4\">Queue</h2><div class=\"table-responsive\"><table class=\"table\"><thead><tr><th>Place</th><th>Name</th><th>Minutes</th></tr></thead><tbody>" + rows + "</tbody></table></div><form method=\"post\"><input type=\"hidden\" name=\"action\" value=\"next\"><button class=\"btn btn-primary\">Call next</button></form></div></section>"
            + "<section class=\"card\"><div class=\"card-body\"><h2 class=\"h4\">Cancel a request</h2><form method=\"post\"><input type=\"hidden\" name=\"action\" value=\"cancel\"><label class=\"form-label\" for=\"code\">Personal code</label><input class=\"form-control mb-3\" id=\"code\" name=\"code\" required><button class=\"btn btn-outline-danger\">Cancel request</button></form></div></section></main></body></html>";
    }
}

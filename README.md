# Layer Line (Group 88, Project 3)

Console MVC app for a 3D-print queue.

```
src/main/java/layerline/
  MODEL       LinkedQueue, PrintRequest, LayerLineModel, JoinResult, StatusEntry, JoinOutcome
  VIEW        ConsoleView
  CONTROLLER  LayerLineController, LayerLineApp
src/test/java/layerline/   JUnit 5 tests for the model (no view)
```

Run without Maven:
```
mkdir out && javac -d out src/main/java/layerline/*.java
java -cp out layerline.LayerLineApp
```
Run tests: `mvn test`

## Web interface

Run with Java 17 and open http://127.0.0.1:8080:

```
mkdir -p out
javac -d out src/main/java/layerline/*.java
java -cp out layerline.LayerLineWebApp
```

Set `PORT` to change the listening port. The server listens on all interfaces;
use a firewall or reverse proxy if needed. Bootstrap styling loads from a CDN.

## Docker Compose

```
docker compose up --build -d
```

Open http://127.0.0.1:8080. Compose binds the published port to localhost;
change the host side of `ports` in `compose.yaml` to use another port. No reverse
proxy is included. Stop with `docker compose down`. Queue data is held in memory
and is lost when the application restarts.

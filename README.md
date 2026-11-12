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

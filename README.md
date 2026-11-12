# Layer Line (Group 88, Project 3)

Console MVC app for a 3D-print queue.

```
src/main/java/layerline/
  MODEL       LinkedQueue, PrintRequest, LayerLineModel, JoinResult, StatusEntry, JoinOutcome
  VIEW        ConsoleView
  CONTROLLER  LayerLineController, LayerLineApp
src/test/java/layerline/   JUnit 5 tests: model (no view) + controller integration
```

### Run the model (no view): JUnit tests

The model is exercised by `LinkedQueueTest` and `LayerLineModelTest`. These
tests build `LinkedQueue` and `LayerLineModel` directly and never create a
`Scanner`, `ConsoleView`, `LayerLineController`, or web server.

- With Maven: `mvn test` (47 tests, all passing: 34 model, 13 controller)
- In Eclipse: right-click `src/test/java` > Run As > JUnit Test

### Run the thin view: console app

```
mkdir out && javac -d out src/main/java/layerline/*.java
java -cp out layerline.LayerLineApp
```

Commands: `join`, `status`, `next`, `cancel`, `quit`. The console view goes
through `LayerLineController`; it never touches the model directly.
`LayerLineControllerTest` runs the console scenarios automatically by feeding
scripted input through a `Scanner` and checking the printed output.

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

## Reflection

### What changed from the spec

Each change lists what the spec said, what we built, why, and what prompted it.

**1. Added a web interface (`LayerLineWebApp`)**
- Spec said: the MVP is console only. A GUI was a stretch goal, not part of Deliverable 3 (spec section 9).
- We built: `LayerLineWebApp`, a small HTTP server using the JDK's built-in `com.sun.net.httpserver`. It supports join, status, next, and cancel in a browser.
- Why: the scope says the GUI board "proves the model does not depend on the view." We wanted that proof early, and the model ran under a second view with zero changes.
- Prompted by: the GUI board stretch goal in our scope.
- Cost: the web app holds a `LayerLineModel` directly and calls it without a controller. Spec section 3 says the view never holds a model reference. It also repeats the controller's message wording. See spec change A below.

**2. Added Docker deployment (`Dockerfile`, `compose.yaml`, `.dockerignore`)**
- Spec said: nothing about deployment. The MVP has no network.
- We built: a Docker image that compiles the sources and runs `LayerLineWebApp`, plus a Compose file that publishes port 8080 on localhost only.
- Why: so any teammate or TA can start the web view with one command, without setting up a JDK.
- Prompted by: change 1. Once there was a web view, we needed an easy way to run it.
- Cost: none to the model. The console MVP still runs without Docker.

**3. Added a Maven build (`pom.xml`)**
- Spec said: no build tool was named.
- We built: a `pom.xml` with JUnit 5 so `mvn test` runs every model test in one step.
- Prompted by: needing all three of us to run the same test suite the same way.

**4. Result-object constructors are package-private**
- Spec said: `JoinResult` and `StatusEntry` are read-only. It did not say who may create them.
- We built: their constructors are package-private, so only model code can create them.
- Why: the spec rule that a personal code only appears in an ADDED result. Outside code cannot fake a `JoinResult` that carries a code.
- Prompted by: implementing the "code never leaks" rule in spec section 4.

**5. Model constants are package-private, not private**
- Spec said: "Fields (all private)" in section 4.
- We built: `MAX_WAITING`, `MIN_MINUTES`, `MAX_MINUTES`, `MAX_NAME_LENGTH`, and `CODE_LENGTH` are `static final` with package access.
- Why: they are constants and cannot be changed. Keeping them visible lets same-package tests use them later instead of hard-coding 40 or 240.
- Prompted by: our own review against the spec while writing this reflection. See spec change B.

**6. `handleJoin` re-prompts only the name after a name error**
- Spec said: `handleJoin` "retries invalid values" (section 5.4).
- We built: minutes are read and checked once by `readMinutes()`. If the model then rejects the name (blank or longer than 20), the controller asks for the name again and keeps the minutes already entered.
- Why: the minutes were already valid. Asking for them again adds nothing.
- Prompted by: running the "Control/oversize names" scenario from spec section 7.4.

**7. Validation stayed where the spec put it**
- Unchanged from spec section 6. The controller checks command words and number text in `run()` and `readMinutes()`. The model checks every rule in `join()` and `cancel()`. `LinkedQueue` only rejects null. Minutes are checked in both places, as the spec planned: the controller re-prompts, and the model throws as a backstop.
- One side effect: the web view has no `readMinutes()`. It relies on the model backstop and catches `NumberFormatException` itself. This showed that putting the rules in the model was the right call.

**8. Tests**
- Spec said: JUnit tests for sections 7.1 to 7.3, plus automated controller integration tests and view display checks (sections 7.4 and 8).
- We built: 16 `LinkedQueueTest` cases and 18 `LayerLineModelTest` cases that cover every 7.1 to 7.3 row. Some 7.3 getter checks are folded into model tests, for example `joinAddsWithSixLetterCode`.
- We also built: `LayerLineControllerTest` (13 tests) automating every spec 7.4 scenario. It feeds input through `new Scanner(String)` and captures `System.out`, so no one has to type. Because `ConsoleView` takes a `Scanner` in its constructor (spec 5.5), we could test the controller without changing any main code.
- Not built yet: separate `ConsoleView` display tests. The controller tests already check that status lines never print a code.
- Prompted by: the 7.4 scenarios were first run by hand, which was slow and easy to skip. Automating them makes them cheap to rerun before every merge.

### Spec changes

We updated our spec to match what we built.

| Spec section | Old text | Change |
|---|---|---|
| A. Section 1 class table, section 3 boundary | The view never holds a model reference. GUI is a stretch goal only. | Add `LayerLineWebApp` (VIEW + CONTROLLER, temporary). It currently calls the model directly. In Deliverable 4 it will be split into a web view and a controller so it follows the same boundary as `ConsoleView`. |
| B. Section 4 fields | All fields private. | Instance fields stay private. Model constants are `static final` with package access. |
| C. Section 5.4 `handleJoin` | Retries invalid values. | Minutes are read once. After a model rejection of the name, only the name is asked again. |
| D. Section 7.4 | Controller and console scenarios are automated tests. | Done as planned in `LayerLineControllerTest`. The "No-view unit testing" row is covered by the two model test files. |
| E. New: deployment | (none) | Docker and Compose run the web view on `127.0.0.1:8080`. The console MVP does not need them. |

### Did the model really stand alone?

Yes.
- `LayerLineModel`, `LinkedQueue`, `PrintRequest`, `JoinResult`, `StatusEntry`, and `JoinOutcome` never import or mention `Scanner`, `System.out`, `ConsoleView`, `LayerLineController`, or anything web related.
- Both test files build the model directly, with no view.
- The strongest evidence is that `LayerLineWebApp` was added without changing a single model line.
- Nothing had to move across the model/view line to make the console run.
- One leak toward presentation: the model's `IllegalArgumentException` messages (for example "Name is too long (maximum 20 characters).") are shown to users word for word by both views. The model does not print them, but it does choose their wording.
- Going the other way, the web app still knows too much about the model (change 1).

### What happened to our unknowns

| Unknown from scope | Status | How |
|---|---|---|
| Removing from the middle of a singly linked queue, especially the tail | Figured out | `LinkedQueue.removeFirst(T)` walks with `prev`/`cur` and moves `tail` back to `prev` when the tail is removed. Tested by `removeFirstHead`, `removeFirstMiddle`, `removeFirstTailThenEnqueue`, `removeFirstOnlyElementThenEnqueue`, and `cancelHeadMiddleTailThenJoin`. |
| Safe console number input | Figured out | `readMinutes()` reads a whole line, trims it, and parses inside a `NumberFormatException` catch. It then checks 1 to 240. `abc`, blank, `1.5`, `99999999999`, `-5`, `0`, and `241` all re-prompt. |
| Unique, hard-to-guess personal codes | Figured out | `makeCode()` builds 6 random letters with `java.util.Random` and retries if the code matches someone in line (about 300 million possible codes). |
| Keeping the view out of the queue | Figured out | `getStatus()` returns a new `List<StatusEntry>` with no codes and no nodes. `Node` is a private nested class. |
| Still open | Open | Anyone who sees a code can cancel that spot, and nothing checks that the person typing `next` is staff. Both are outside the MVP scope. |

### What doesn't work yet

- No separate `ConsoleView` unit tests yet (the view is covered only through `LayerLineControllerTest`).
- `LayerLineWebApp` skips the controller and has no tests.
- The web view has no login and listens on all interfaces when run outside Docker, so it is only meant for local use. Bad web input (blank name, non-numeric minutes, unknown code) shows a message instead of crashing, but only by manual testing.
- All MVP features work, and no bad-input case from the scope crashes the console app.

## Next Steps

### 1. Finish the MVP (in order)

| # | Item | Owner |
|---|---|---|
| 1 | Write `ConsoleViewTest`: `showStatus` prints 1-based places and never a code; an empty list prints "Line is empty"; `prompt` returns null on EOF; a null `Scanner` throws. | Karthik |
| 2 | Split `LayerLineWebApp` into a web view and a controller that calls the model only through public methods (spec change A). | Brian |
| 3 | Update the spec document with spec changes A to E. | Ajeet |

### 2. Change feature: Reprint at the front

**What it does:** if a student's print fails after they were called with `next`, they type `reprint` and their personal code. They rejoin the line at place 1, once. This is the only way to get to the front, so the fairness rule stays simple: one free retry per failed print.

**Why it is non-trivial:** it adds new model state (who was served and whether they already used their reprint) and new queue logic (insert at the head). Users reach it through a new controller command.

**Rules:**
- Only a student who was already called can reprint.
- Each code can reprint once.
- A reprint is rejected if the line is full (40) or the same name is already waiting.
- The code is checked the same way as in `cancel`: trimmed and uppercased.

**Predicted changes:**

| Class | Change? | Methods |
|---|---|---|
| `LinkedQueue<T>` (model) | Yes | New `enqueueFront(T value)`, O(1). Must set `tail` when the queue was empty. Rejects null. |
| `LayerLineModel` (model) | Yes | New field: a list of served requests that have not used their reprint. `callNext()` adds the served request to that list. New `reprint(String personalCode) → boolean`. |
| `LayerLineController` (controller) | Yes | New `"reprint"` case in `run()`, new `handleReprint()`, and the unknown-command message adds `reprint`. |
| `LayerLineWebApp` (or its new controller) | Yes | New `reprint` action in `handle()` and a form in `page()`. |
| `ConsoleView` | **No** | `prompt`, `showMessage`, and `showStatus` already cover it. |
| `PrintRequest` | **No** | Stays immutable. The model tracks reprint use, not the request. |
| `StatusEntry`, `JoinResult`, `JoinOutcome` | **No** | The reprinted student shows up in `getStatus()` like anyone else. |
| `LayerLineApp` | **No** | Wiring does not change. |

We will compare this table to what actually changed in Deliverable 4.

### 3. Stretch goals

| Stretch goal | Decision |
|---|---|
| Reprint at the front | **Committed**, as our change feature. |
| GUI board | **Committed**, as cleanup only. The web UI already exists. We will route it through a controller (MVP item 2). |
| Separate PLA and resin lines | **Dropped.** It would need a second queue and a routing rule, and we do not have time with the reprint feature. |
| Save and load | **Dropped.** File I/O adds error cases we cannot fully test this round. |

### 4. New tests needed

- `LinkedQueue.enqueueFront`:
  - On an empty queue, head == tail and size 1, and a later `enqueue` goes behind it.
  - On a non-empty queue, the new item is first in `snapshot()`.
  - `enqueueFront(null)` throws `IllegalArgumentException` and leaves the queue unchanged.
- `LayerLineModel.reprint`:
  - Join A, B; `callNext()` (A); `reprint(A's code)` returns true and status is [A, B].
  - Lowercase code with spaces works.
  - A second reprint with the same code returns false.
  - A code of someone still waiting returns false.
  - Unknown or blank code returns false. Null throws `IllegalArgumentException`.
  - Reprint when the line has 40 returns false, size stays 40.
  - Reprint when the same name already rejoined returns false.
- `LayerLineControllerTest`: `reprint` with a good code, a bad code, and EOF at the code prompt.
- Web controller (after the split): join, cancel, next, and reprint actions, plus a non-numeric minutes value.

### 5. Who does what

| Item | Owner |
|---|---|
| `enqueueFront` + `LinkedQueueTest` additions | Brian |
| `reprint`, served list, `callNext` change + `LayerLineModelTest` additions | Brian |
| `handleReprint`, `run()` case + `LayerLineControllerTest` | Ajeet |
| `ConsoleViewTest`, manual bad-input script for reprint | Karthik |
| Web view/controller split + reprint form | Brian, Karthik |
| Spec update and Deliverable 4 prediction-vs-actual comparison | Ajeet |
| Code review before merge | All three, reviewing a layer they did not write |

## Attribution

| Class / test file | Layer | Who worked on it and how |
|---|---|---|
| `LinkedQueue.java` | Model | Brian wrote it. Karthik integrated it. Ajeet reviewed the `removeFirst` pointer cases. |
| `PrintRequest.java` | Model | Brian wrote it. Karthik reviewed it. |
| `LayerLineModel.java` | Model | Brian wrote it, including `makeCode` and name cleaning. Ajeet reviewed the validation against spec section 6. |
| `JoinResult.java` | Model | Brian wrote it. Ajeet reviewed it. |
| `StatusEntry.java` | Model | Brian wrote it. Karthik reviewed it (no code field reaches the view). |
| `JoinOutcome.java` | Model | Brian wrote it. |
| `ConsoleView.java` | View | Karthik wrote it and ran the manual display checks. Ajeet reviewed it. |
| `LayerLineController.java` | Controller | Ajeet wrote it, including `readMinutes` and the command handlers. Karthik revised and integrated it. Ajeet tested the spec 7.4 scenarios by hand. |
| `LayerLineApp.java` | Controller | Ajeet wrote it. Karthik integrated it. |
| `LayerLineWebApp.java` | View + controller | Brian wrote it. Brian also wrote the `Dockerfile` and `compose.yaml`. |
| `LinkedQueueTest.java` | Test | Brian wrote it. Karthik reviewed it against spec 7.1. |
| `LayerLineControllerTest.java` | Test | Ajeet wrote it with GenAI help (Claude drafted the first version from spec 7.4; Ajeet checked each scenario against the spec and ran it). |
| `LayerLineModelTest.java` | Test | Brian and Karthik wrote it. Ajeet reviewed it against spec 7.2 and 7.3. |
| `README.md` | Docs | Karthik wrote the run instructions. Brian added the web and Docker sections. Ajeet drafted Reflection, Next Steps, and Attribution with GenAI help (see below), and the team reviewed them. |

### GenAI note

Tools: ChatGPT and Claude. The team checked everything below before keeping it. Anything not listed here was done without GenAI.

**ChatGPT (Deliverable 2, design)**
- We asked how to give each person in line a random personal code that never repeats. It suggested `SecureRandom` plus a `Set` of every code already given out. Ajeet brought this to the team. Claude later simplified it (next section).

**Claude (Deliverable 2, design)**
- We pasted in parts of our draft spec and asked whether `SecureRandom` plus a `Set` was more than this project needs. It suggested `java.util.Random` with a check against the people currently in line. Brian updated the model fields to match. This became `LayerLineModel.makeCode()`.
- We asked whether any of our method signatures used things not covered in class. It pointed out `removeFirst(Predicate)` and suggested `removeFirst(T target)`. That is the signature in `LinkedQueue`.

**Claude (Deliverable 3, this prototype)**
- **`LayerLineControllerTest` (13 tests):** Claude wrote the first version from our spec section 7.4 scenarios. The tests feed scripted input through `new Scanner(String)` and capture `System.out`, so no main code had to change. They cover: null constructor arguments, the join/status/next/cancel workflow, bad minutes (`abc`, blank, `1.5`, `0`, `241`, a huge number), duplicate names in different casing, a wrong cancel code and `next` on an empty line, a full line (40), control-character and over-20-character names, blank names re-prompting only the name, `quit` typed as a name, cancelling from the head/middle/tail, unknown commands and mixed-case commands, and end of input at the command, join and cancel prompts. Ajeet checked every assertion against spec 7.4 and ran the suite (47 of 47 pass).
- **README run instructions:** Claude corrected the commands for running the model tests (`mvn test` and Eclipse) and the console app, and the test counts.
- **README Reflection:** Claude wrote the first draft of the eight "What changed from the spec" items, the spec changes table (A to E), "Did the model really stand alone?", the unknowns table, and "What doesn't work yet", working from our code, spec and scope. While drafting it, it caught and fixed one wrong claim of its own (it first said the web view did not escape names; it does).
- **README Next Steps:** Claude wrote the first draft of the MVP to-do list, the "Reprint at the front" change feature (the feature itself comes from our scope), the table predicting which classes and methods will and will not change, the stretch goal decisions, the new test list, and the owner table.
- **README Attribution:** Claude wrote the first draft of this table from our git history and the spec's division of work. Each teammate checked their own rows.
- **Cleanup:** Claude removed leftover team notes from the README and fixed a wrong item number in the stretch goals table ("MVP item 3" to "MVP item 2").

**Not GenAI:** all main Java code (`LinkedQueue`, `LayerLineModel`, `PrintRequest`, `JoinResult`, `StatusEntry`, `JoinOutcome`, `ConsoleView`, `LayerLineController`, `LayerLineApp`, `LayerLineWebApp`), `LinkedQueueTest`, `LayerLineModelTest`, the Maven build, and the Docker files.

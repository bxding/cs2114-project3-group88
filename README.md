# Layer Line (Group 88, Project 3)

Console MVC app for a 3D-print queue, plus a small web view. Deliverable 4 adds
the change feature **Reprint at the front**.

```
src/main/java/layerline/
  MODEL       LinkedQueue, PrintRequest, LayerLineModel, JoinResult, StatusEntry, JoinOutcome
  VIEW        ConsoleView, LayerLineWebApp
  CONTROLLER  LayerLineController, WebController, LayerLineApp
src/test/java/layerline/   JUnit 5 tests: model (no view) + view and controller
```

### Run the tests

74 tests, all passing:

- **Model, no view (47):** `LinkedQueueTest` (21) and `LayerLineModelTest` (26).
  They build `LinkedQueue` and `LayerLineModel` directly and never create a
  `Scanner`, view, controller, or web server.
- **View and controller (27):** `LayerLineControllerTest` (16), `ConsoleViewTest` (6),
  `WebControllerTest` (5).

How to run them:

- With Maven: `mvn test`
- In Eclipse: right-click `src/test/java` > Run As > JUnit Test

### Compile and run the console app

```
mkdir -p out && javac -d out src/main/java/layerline/*.java
java -cp out layerline.LayerLineApp
```

Commands: `join`, `status`, `next`, `cancel`, `reprint`, `quit`. Every command goes
through `LayerLineController`; `ConsoleView` never touches the model.

- `join`: name and print minutes (1 to 240). Prints your 6-letter personal code once.
- `next`: calls the first student in line.
- `cancel`: personal code; removes you from the line.
- `reprint`: personal code; if your print failed after you were called, you go back
  to place 1, once per code.

## Web interface

Run with Java 17 and open http://127.0.0.1:8080:

```
mkdir -p out
javac -d out src/main/java/layerline/*.java
java -cp out layerline.LayerLineWebApp
```

The page has join, call next, cancel and reprint forms. `LayerLineWebApp` only parses
forms and renders HTML; every action goes through `WebController`.

Set `PORT` to change the listening port. The server listens on all interfaces;
use a firewall or reverse proxy if needed. Bootstrap styling loads from a CDN.

## Docker Compose

```
docker compose up --build -d
```

Open http://127.0.0.1:8080. Compose binds the published port to localhost;
change the host side of `ports` in `compose.yaml` to use another port. No reverse
proxy is included. Stop with `docker compose down`. Queue data is held in memory
and is lost when the application restarts. The Docker build runs all JUnit tests.

## Reflection

### How did our design absorb the change?

The change feature is **Reprint at the front**: a student whose print fails after
they were called with `next` types `reprint` and their personal code, and goes back
to place 1, once per code. This is the feature we named in our Deliverable 3 Next Steps.

**Classes the feature actually touched, compared with our D3 prediction:**

| Class | D3 prediction | What actually happened | Match? |
|---|---|---|---|
| `LinkedQueue<T>` | Changes: new `enqueueFront(T)`, O(1), sets `tail` when empty, rejects null | Exactly that | Yes |
| `LayerLineModel` | Changes: a list of served requests, `callNext()` adds to it, new `reprint(String) → boolean` | All of that, plus a second queue `reprinted`, a change to `makeCode()`, and `cancel()` clearing the reprint marker | Partly (see surprises) |
| `LayerLineController` | Changes: `"reprint"` case in `run()`, new `handleReprint()`, unknown-command message | Exactly that, plus the join message now says the code is needed to "cancel or reprint" | Yes |
| `LayerLineWebApp` (or its new controller) | Changes: `reprint` action and a form | A `reprint` action and form, and the page now goes through the new `WebController` (that split was D3 MVP item 2) | Yes |
| `ConsoleView` | No change | No change | Yes |
| `PrintRequest` | No change; stays immutable | No change | Yes |
| `StatusEntry`, `JoinResult`, `JoinOutcome` | No change | No change | Yes |
| `LayerLineApp` | No change | No change | Yes |

**No class we predicted would stay unchanged had to change.**

**What surprised us:**

1. **"Once per code" needed more state than we predicted.** We planned one list of
   served students. But a student who reprints is called with `next` again, and
   `callNext()` would put them back on the served list, giving them a second reprint.
   We added a second queue, `reprinted`, so `callNext()` skips them.
   `LayerLineModelTest.reprintOnlyOncePerCode` caught this.
2. **Codes had to stay unique longer.** In D3 a code only mattered while you were
   waiting, so `makeCode()` only checked the line. Now a called student's code still
   works for a reprint, so a new student could have been given the same code.
   `makeCode()` now checks both the line and the served queue.
3. **We reused our own `LinkedQueue` for both new lists** instead of `ArrayList`.
   `removeFirst` uses `equals`, and `PrintRequest` does not override it, so it
   matches the exact object. That is what we want.
4. **`cancel()` had one new line:** it clears the `reprinted` marker for the
   cancelled request, so the list does not keep old entries.

### Did the change cross the model/view line?

**No.**

- **`ConsoleView` did not change and learned nothing new.** A reprinted student shows
  up in `getStatus()` like anyone else, as a `StatusEntry` with no code.
- **Both controllers learned one new model method, `reprint(String)`.** All the rules
  (called before, once per code, full line, same name already waiting, trim and
  uppercase) live in the model.
- **The web view is now on the right side of the line.** In D3, `LayerLineWebApp` held
  a `LayerLineModel` and called it directly. Now it holds only a `WebController`, and
  the page re-reads `controller.status()` on every request. Neither view keeps a copy
  of model state: `ConsoleView` holds only its `Scanner`, and `LayerLineWebApp` holds
  only its controller.

**Where it rippled more than it should have:** the failure message. Because
`reprint` returns a `boolean` (as our D3 prediction said), the controllers cannot
tell the user *which* rule failed, so both print the same general message. That
text is also written twice, once in `LayerLineController` and once in
`WebController`.

### What changed from our D3 plan

| Area | D3 plan | What we did | Why |
|---|---|---|---|
| Feature | Reprint at the front | Same feature | No switch |
| Signatures | `enqueueFront(T)`, `reprint(String) → boolean` | Same | Kept the prediction so the comparison is fair |
| Model state | One list of served requests | Two queues: `served` and `reprinted` | Surprise 1 above |
| `makeCode` | Not mentioned | Private `makeCode(List)` became `makeCode()`, checking the line and `served` | Surprise 2 above |
| Validation | Reject: never called, already reprinted, full line, same name waiting; trim and uppercase | All of these, plus blank code returns false and `null` throws `IllegalArgumentException` | Same convention as `cancel` |
| Web view | Split into a web view and a controller | New `WebController`; `LayerLineWebApp` only parses forms and renders HTML | D3 MVP item 2 |
| Tests | `enqueueFront`, `reprint`, controller reprint cases, web controller tests | All of these, plus interleaved enqueue/dequeue order tests and the `ConsoleViewTest` file left over from D3 | Rubric and our D3 list |
| Manual reprint bad-input script | Karthik to write one | Replaced by automated tests in `LayerLineControllerTest` and `WebControllerTest` | Faster to rerun |

### What doesn't work yet

- **Reprint does not say why it was refused.** The message lists all the rules instead.
- **The served queue only shrinks when someone reprints.** It grows by one per
  student called during a run. That is fine for one lab session, and a restart clears it.
- **No access control.** Anyone who sees a code can cancel or reprint that spot,
  and anyone can type `next`. This is outside our scope.
- **The HTTP part of `LayerLineWebApp` has no automated tests.** `WebController` is
  tested; the form parsing and HTML were checked by hand.
- **Nothing is saved.** The line resets when the program restarts.

### What would make the next feature cheaper

**Return a result object from `reprint`, and keep all user-facing text in one place.**

- A `ReprintResult` with an outcome enum (like `JoinResult` and `JoinOutcome`) would
  let both controllers say exactly why a reprint failed.
- One shared message class used by both `LayerLineController` and `WebController`
  would stop every new feature from needing the same text written twice.

With both in place, a feature like separate PLA and resin lines would add model
logic and one set of messages, not two.

## Next Steps

### 1. What is left for the final build (in order)

| # | Item | Owner |
|---|---|---|
| 1 | Add `ReprintOutcome` and a `ReprintResult` so `reprint` says why it failed (model + tests) | Brian |
| 2 | Use it in `handleReprint` and update `LayerLineControllerTest` | Ajeet |
| 3 | Use it in `WebController` and update `WebControllerTest` | Karthik |
| 4 | Move shared user-facing messages into one class used by both controllers | Karthik |
| 5 | Remove served entries once the line has moved on far enough, with a test | Brian |
| 6 | Update the spec document and UML: `enqueueFront`, `reprint`, `served`/`reprinted`, `WebController` | Ajeet |
| 7 | Rehearse the 3-minute demo below twice, timed | All three |

### 2. Stretch goals

| Stretch goal | Decision |
|---|---|
| Reprint at the front | **Committed and built** (this deliverable's change feature) |
| GUI board | **Committed and built**: the web view, now through `WebController` |
| Separate PLA and resin lines | **Dropped**: needs a second line and a routing rule; not enough time with items 1 to 5 |
| Save and load | **Dropped**: file errors add cases we cannot fully test this round |

### 3. Our 3-minute demo

Console app, one person typing, one person talking.

| Time | What we show |
|---|---|
| 0:00 to 0:20 | Start `LayerLineApp`; point out the command list |
| 0:20 to 0:50 | `join` Sam (30), Lee (45), Kim (10); each code is shown once; `status` shows no codes |
| 0:50 to 1:10 | Bad input: minutes `abc` and `241` re-prompt; a blank name re-prompts only the name |
| 1:10 to 1:25 | `next` calls Sam |
| 1:25 to 2:05 | **New feature:** `reprint` with a wrong code is refused; `reprint` with Sam's code in lowercase puts Sam at place 1; a second `reprint` is refused |
| 2:05 to 2:25 | `cancel` Lee from the middle with Lee's code; `status` |
| 2:25 to 2:45 | Web page: the same reprint form, going through `WebController` |
| 2:45 to 3:00 | Test run: 74 tests passing, 47 of them on the model with no view |

| Demo part | Owner |
|---|---|
| Console typing (0:00 to 2:25) | Ajeet |
| Talking through the model and the reprint rules | Brian |
| Web page and test run (2:25 to 3:00) | Karthik |

## Attribution

| Class / test file | Layer | Who worked on it and how |
|---|---|---|
| `LinkedQueue.java` | Model | Brian wrote it. Karthik integrated it. Ajeet reviewed the `removeFirst` pointer cases. D4: Brian wrote `enqueueFront` with GenAI help (Claude helped write it from our D3 prediction). |
| `PrintRequest.java` | Model | Brian wrote it. Karthik reviewed it. Unchanged in D4. |
| `LayerLineModel.java` | Model | Brian wrote it, including `makeCode` and name cleaning. Ajeet reviewed the validation against spec section 6. D4: Brian wrote `reprint` and the `served` and `reprinted` queues. The `callNext`/`cancel`/`makeCode` changes and the `findByCode`/`findByName` helpers were GenAI-assisted (Claude implemented them); Brian reviewed and owns them. |
| `JoinResult.java` | Model | Brian wrote it. Ajeet reviewed it. Unchanged in D4. |
| `StatusEntry.java` | Model | Brian wrote it. Karthik reviewed it (no code field reaches the view). Unchanged in D4. |
| `JoinOutcome.java` | Model | Brian wrote it. Unchanged in D4. |
| `ConsoleView.java` | View | Karthik wrote it and ran the manual display checks. Ajeet reviewed it. Unchanged in D4. |
| `LayerLineWebApp.java` | View | Brian wrote it, plus the `Dockerfile` and `compose.yaml`. D4: Brian and Karthik split it into view + `WebController` and added the reprint form. |
| `LayerLineController.java` | Controller | Ajeet wrote it, including `readMinutes` and the command handlers. Karthik revised and integrated it. D4: Ajeet wrote `handleReprint` and the `reprint` case. |
| `WebController.java` | Controller | New in D4. Karthik wrote it by moving the web app's logic out of `LayerLineWebApp`. |
| `LayerLineApp.java` | Controller | Ajeet wrote it. Karthik integrated it. Unchanged in D4. |
| `LinkedQueueTest.java` | Test | Brian wrote it. Karthik reviewed it against spec 7.1. D4: Brian wrote the 3 `enqueueFront` tests and the 2 interleaved order tests; Claude refined the interleaved order tests. |
| `LayerLineModelTest.java` | Test | Brian and Karthik wrote it. Ajeet reviewed it against spec 7.2 and 7.3. D4: Brian wrote the 8 reprint tests; Claude refined them. |
| `LayerLineControllerTest.java` | Test | GenAI-assisted: Claude drafted the first version from spec 7.4; Ajeet checked each scenario against the spec and ran it. D4: Ajeet wrote the 3 reprint tests. |
| `ConsoleViewTest.java` | Test | New in D4. Karthik wrote it from spec section 8. |
| `WebControllerTest.java` | Test | New in D4. Karthik wrote it. |
| `README.md` | Docs | Karthik wrote the run instructions. Brian added the web and Docker sections. D3 Reflection, Next Steps and Attribution were GenAI-assisted (Claude drafted them); Ajeet edited them and the team reviewed them. The team wrote the D4 run instructions, Reflection, Next Steps and Attribution. |

### GenAI note

Tools: ChatGPT and Claude. The team checked everything below before keeping it. Anything not listed here was done without GenAI.

**ChatGPT (Deliverable 2, design)**
- We asked how to give each person in line a random personal code that never repeats. It suggested `SecureRandom` plus a `Set` of every code already given out. Ajeet brought this to the team. Claude later simplified it (next section).

**Claude (Deliverable 2, design)**
- We pasted in parts of our draft spec and asked whether `SecureRandom` plus a `Set` was more than this project needs. It suggested `java.util.Random` with a check against the people currently in line. Brian updated the model fields to match. This became `LayerLineModel.makeCode()`.
- We asked whether any of our method signatures used things not covered in class. It pointed out `removeFirst(Predicate)` and suggested `removeFirst(T target)`. That is the signature in `LinkedQueue`.

**Claude (Deliverable 3, this prototype)**
- **`LayerLineControllerTest` (13 tests):** Claude wrote the first version of this file, covering: null constructor arguments, the join/status/next/cancel workflow, bad minutes (`abc`, blank, `1.5`, `0`, `241`, a huge number), duplicate names in different casing, a wrong cancel code and `next` on an empty line, a full line (40), control-character and over-20-character names, blank names re-prompting only the name, `quit` typed as a name, cancelling from the head/middle/tail, unknown commands and mixed-case commands, and end of input at the command, join and cancel prompts. Ajeet checked every assertion against spec 7.4 and ran the suite (47 of 47 pass).
- **README run instructions:** Claude corrected the commands for running the model tests (`mvn test` and Eclipse) and the console app, and the test counts.
- **README Reflection:** Claude wrote "What changed from the spec" items, the spec changes table (A to E), "Did the model really stand alone?", the unknowns table, and "What doesn't work yet", working from our code, spec and scope. While drafting it, it caught and fixed one wrong claim of its own (it first said the web view did not escape names; it does).
- Claude also improved the structure of the readme and code for better readability.
- **Cleanup:** Claude removed leftover team notes from the README and fixed a wrong item number in the stretch goals table ("MVP item 3" to "MVP item 2").

**Claude (Deliverable 4, second prototype)**
- **`LinkedQueue.enqueueFront`:** Claude helped us write it from our D3 prediction (O(1), sets `tail` when the queue was empty, rejects `null`).
- **`LayerLineModel` reprint feature:** we wrote `reprint(String)` and the `served` and `reprinted` queues. Claude implemented the change to `callNext()` that records who was called, the change to `cancel()` that clears the reprint marker, the change to `makeCode()` so codes stay unique against students who can still reprint, and the `findByCode` / `findByName` helpers. Brian reviewed each change against our D3 rules.
- **Tests:** Claude refined the interleaved enqueue/dequeue order tests in `LinkedQueueTest` and the 8 reprint tests in `LayerLineModelTest`. We wrote the first versions and checked each refined assertion before keeping it.

**Not GenAI:** the Deliverable 2 and 3 main Java code (`LinkedQueue`, `LayerLineModel`, `PrintRequest`, `JoinResult`, `StatusEntry`, `JoinOutcome`, `ConsoleView`, `LayerLineController`, `LayerLineApp`, `LayerLineWebApp`), the Deliverable 3 versions of `LinkedQueueTest` and `LayerLineModelTest`, the Maven build, and the Docker files. In Deliverable 4 we wrote everything not listed above ourselves: the reprint case and `handleReprint()` in `LayerLineController`, `WebController` and the `LayerLineWebApp` split and reprint form, the 3 `enqueueFront` tests, the 3 reprint tests in `LayerLineControllerTest`, `ConsoleViewTest`, `WebControllerTest`, and the Deliverable 4 README sections.

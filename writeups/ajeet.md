# Individual writeup: Ajeet Bondugula

My parts: the scope's stretch goals (including reprint), spec sections 6 and 9,
`LayerLineController` (`readMinutes`, `handleReprint`), `LayerLineApp`, integration,
and the README attribution.

## What changed from the spec, and why

Spec section 4 checked new codes only against students in line. After reprint, a
called student still needs their code, so `makeCode()` also checks the `served`
queue. In D3 the web view called the model directly, breaking our own rule, so in D4
we split out `WebController`. And `reprint` went from a `boolean` to a
`ReprintOutcome` enum, because a `boolean` could not tell a student which rule failed.

## What "separation of concerns" means to me now

In Week 10 it meant "model, view and controller in different files." Now it means
"each rule has one home." The 40 cap, name rules and reprint rules live only in
`LayerLineModel`, and the result messages both controllers share live in `Messages`.
In D4 we had separate files and still wrote the reprint refusal twice. Also, in spec
section 6 I said only a text console needs number parsing, but our web form sends
minutes as text too, so `WebController` parses them as well.

## Where keeping the model and view apart paid off

49 of our 78 tests test the model layer with no view: no `Scanner`, console or server.
`LayerLineModelTest.reprintOnlyOncePerCode` caught the "second reprint" bug as one
failing assertion. Adding the web page changed nothing in the model, and adding
reprint changed nothing in `ConsoleView`.

## What the D4 feature revealed, and what I would draw differently

Our results were too thin: `join` returned a `JoinResult`, but `cancel` and `reprint`
returned `boolean`, so controllers had to guess why something failed. Starting over,
I would give every model command an outcome enum, add a shared message class from day
one, and draw model state as "who is waiting" plus "who can still use their code."

## Where MVC felt like overhead, and how we handled it

Some controller methods only forward a call: `WebController.status()` just returns
`model.getStatus()`. We kept them because two views share one model. Where overhead
had no payoff we cut it: a planned `ReprintResult` would only have wrapped the enum.

## How my approach to GenAI shifted across the three projects

Project 1: I did not use GenAI. Project 2: I used Claude to draft parts of our spec and
refine my `CheckoutRecord` tests, but our first build contradicted the spec, and Claude
Code made most of the fixes afterward. We reviewed its changes instead of deciding
first.

In Project 3 I used it earlier and in smaller pieces. In the scope I asked ChatGPT to
explain safe number input and linked-list cancel without writing the code, then wrote
`readMinutes` myself. In D3 Claude drafted `LayerLineControllerTest`, and I checked
every assertion against spec 7.4. In D4 I wrote `handleReprint` myself. It found
bad-input cases I would have skipped, but it also made confident wrong claims, so
everything it wrote got checked.

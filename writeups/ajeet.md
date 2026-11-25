# Individual writeup: Ajeet Bondugula

My parts of Layer Line: the scope overview and stretch goals (including reprint at
the front), spec sections 6 (where validation lives) and 9 (revised scope),
`LayerLineController` (`readMinutes`, `handleReprint`), `LayerLineApp`, integration,
the controller test review against spec 7.4, and the README attribution.

## What changed from the spec, and why

First, the personal code: spec section 4 checked a new code only against students in
line. Once reprint existed, a called student still needed their code, so `makeCode()`
now also checks the `served` queue. Second, the web view: in D3 `LayerLineWebApp`
called the model directly, breaking our own spec's rule, so in D4 we split out
`WebController`. Third, reprint: we predicted one list and a `boolean` return. It
needed a second queue (`reprinted`) so "once per code" held, and in D5 the `boolean`
became a `ReprintOutcome` enum because a `boolean` could not tell a student which
rule failed.

## What "separation of concerns" means to me now

In Week 10 I read it as "put the model, view and controller in different files."
Now I read it as "each piece of knowledge has exactly one home." The 40-person cap,
name rules and reprint rules live only in `LayerLineModel`; user-facing text lives
only in `Messages`. Files were never the point: in D4 we had separate files and still
wrote the reprint refusal sentence twice, once per controller.

One Week 10 claim did not survive. In spec section 6 I wrote that a GUI with a number
box would not have number-parsing errors. Our web form still sends minutes as text,
so `WebController.join` needs the same `parseInt` check as `readMinutes`. Parsing
still stays out of the model, but it is not only a console problem.

## Where keeping the model and view apart paid off

49 of our 78 tests build `LayerLineModel` with no `Scanner`, console or server, so a
model bug shows up as one failing assertion. `LayerLineModelTest.reprintOnlyOncePerCode`
found the "second reprint" bug that way. Adding the web page changed nothing in the
model, and adding reprint changed nothing in `ConsoleView`.

## What the D4 feature revealed, and what I would draw differently

Our model's results were too thin. `join` returned a rich `JoinResult`, but `cancel`
and `reprint` returned `boolean`, so each controller had to guess why something
failed. If I started over I would draw every model command returning an outcome enum,
one message class in the controller layer, and model state as "who is waiting" plus
"who can still use their code," since a code outlives a student's time in line.

## Where MVC felt like overhead, and how we handled it

Some controller methods only forward a call: `WebController.status()` just returns
`model.getStatus()`. We accepted that because it paid off once we had two views.
Where overhead had no payoff, we cut it: a planned `ReprintResult` class would only
have wrapped the enum, so `reprint` returns the enum directly.

## How my approach to GenAI shifted across the three projects

Project 1 (Escape from Slusher Tower): I did not use GenAI. I wrote `Game`, `Combat`
and their tests myself.

Project 2 (library catalogue) showed me the cost of using it late. I used Claude to
draft parts of our spec and refine my `CheckoutRecord` tests, but our first build
contradicted six decisions in the spec. Claude Code then compared the build to the
spec and made most of the fixes, and caught an O(n²) `getItemsByTitle`. It worked,
but we reviewed GenAI changes after the fact instead of deciding first.

In Project 3 I used it earlier, in smaller pieces, with a named owner for each. In the
scope I asked ChatGPT two separate questions (safe minutes input, and how head and
tail change when cancelling the first, middle, last or only node) and asked it to
explain without writing the solution. I coded `readMinutes` myself; its most useful
output was the edge-case list. In D3 Claude drafted `LayerLineControllerTest`, and I
checked every assertion against spec 7.4 before keeping it. In D4 I wrote
`handleReprint` and its tests myself. What went well: it found bad-input cases I
would have skipped. What went poorly: it made confident wrong claims, like saying
our web view did not escape names when it did, so everything it wrote got checked.

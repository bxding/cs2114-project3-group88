# Individual writeup: Ajeet Bondugula

My parts of Layer Line: `LayerLineController` (the command loop, `readMinutes`,
`handleReprint`), `LayerLineApp`, the controller test review against spec 7.4, and
the Next Steps and Attribution sections of the README.

## What changed from the spec, and why

Three changes stand out. First, the personal code: our D2 spec used `SecureRandom`
plus a set of every code ever issued. That was more machinery than a lab queue needs,
so `makeCode()` just checks the codes that can still be used. Second, the web view:
in D3 `LayerLineWebApp` called the model directly, which broke the rule our own spec
set. In D4 we split out `WebController`. Third, reprint. We predicted one list of
served students and a `boolean` return. The real version needed a second queue
(`reprinted`) so "once per code" held, and in D5 the `boolean` became a
`ReprintOutcome` enum because the `boolean` could not tell a student which rule
failed. We planned a `ReprintResult` class too, but it would only have wrapped the
enum, so we dropped it.

## What "separation of concerns" means to me now

In Week 10 I read it as "put the model, view and controller in different files."
Now I read it as "each piece of knowledge has exactly one home." The 40-person cap,
the name rules and the reprint rules live only in `LayerLineModel`. Text the user
reads lives only in `Messages`. Reading and printing lives only in the views. Files
were never the point; in D4 we had separate files and still had the reprint
refusal sentence written twice, once per controller.

## Where keeping the model and view apart paid off

Testing and the second view. 49 of our 78 tests build `LayerLineModel` with no
`Scanner`, console or server, so a model bug shows up as one failing assertion
instead of a wrong line in captured output. `LayerLineModelTest.reprintOnlyOncePerCode`
found the "second reprint" bug that way. When we added the web page, the model did
not change at all, and when we added reprint, `ConsoleView` did not change at all.

## What adding the D4 feature revealed, and what I would draw differently

It revealed that our model's results were too thin. `join` returned a rich
`JoinResult`, but `cancel` and `reprint` returned `boolean`, so every controller had
to guess why something failed. It also revealed duplication between controllers,
which is why `Messages` exists now. If I started over I would draw every model
command returning an outcome enum from day one, draw one message class in the
controller layer, and draw the model's state as "who is waiting" and "who can still
act on their code" from the start, since a code's lifetime turned out to be longer
than a student's time in line.

## Where MVC felt like overhead, and how we handled it

For a five-command console app, routing everything through a controller sometimes
meant a method that only forwarded a call: `WebController.status()` just returns
`model.getStatus()`. And `handleStatus()` only calls `refreshStatus()`. We accepted
that small cost because the moment we had two views it paid for itself. Where the
overhead had no payoff, we cut it: `ReprintResult` would have been a class with one
field, so `reprint` returns the enum directly.

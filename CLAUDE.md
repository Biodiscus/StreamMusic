# StreamMusic

Android music streaming app. Package `net.biodiscus.streammusic`, minSdk 24, targetSdk 37, Kotlin, Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`).

## Android skill

The `.claude/skills/claude-android-skill/` folder in this repo is the source of truth for Android architecture, Compose, modularization, Gradle and testing. Always follow it when creating or changing code.

@.claude/skills/claude-android-skill/SKILL.md

Before working on a topic, read the matching reference first:

| Working on | Read |
|---|---|
| Layers, repositories, ViewModels, UiState | `.claude/skills/claude-android-skill/references/architecture.md` |
| Screens, state hoisting, navigation, theming, previews | `.claude/skills/claude-android-skill/references/compose-patterns.md` |
| New modules, dependency rules between modules | `.claude/skills/claude-android-skill/references/modularization.md` |
| Build files, convention plugins, version catalog | `.claude/skills/claude-android-skill/references/gradle-setup.md` |
| Writing tests, test doubles | `.claude/skills/claude-android-skill/references/testing.md` |

`.claude/skills/claude-android-skill/scripts/generate_feature.py` scaffolds a feature module. Prefer it over writing the boilerplate by hand.

The project's own files win over the skill where they differ:
- The skill's example version numbers are placeholders. Take real versions from `gradle/libs.versions.toml`.
- The app is still close to the Android Studio template (single `:app` module, AppCompat/Material). Hilt, Room, Compose and multi-module are the target architecture, not something to assume is already set up. Check the build files, and add the dependency or module properly when a task needs it.

## Commands

```bash
./gradlew testDebugUnitTest          # JVM unit tests (run after every change)
./gradlew testDebugUnitTest --tests "net.biodiscus.streammusic.SomeTest"
./gradlew connectedDebugAndroidTest  # instrumented tests, needs a device or emulator
./gradlew assembleDebug              # build
./gradlew lintDebug                  # lint
```

## Clean Code

Clean Code is the main quality bar in this project. When in doubt, choose the more readable option.

**Write for a junior developer.** Every piece of code must be understandable by a developer with little experience who is new to this codebase. They should be able to read a method and know what it does without asking anyone.
- Choose the plain, obvious solution over the clever one. No dense one-liners, deeply chained operators, or tricks that need a second read. Use a named intermediate value or an extracted method instead.
- Use well-known patterns and the standard library before inventing abstractions. Add an abstraction only when it removes real duplication or complexity today, not for a future that may never come.
- Use domain words from the music world (`Track`, `Playlist`, `PlaybackPosition`), not abbreviations or generic names like `data`, `item`, `manager`, `helper`, `util`.
- Avoid advanced language features when a simple one does the job: heavy generics, operator overloading, reflection, nested scope functions (`let`/`run`/`apply`/`also` stacked), and complex `Flow` operator chains without named steps.
- Keep nesting shallow. Use early returns and small methods instead of deep `if`/`when`/loop nesting.
- When you do use an advanced concept that the code needs (a coroutine operator, a Compose side effect, a Gradle convention plugin), isolate it in a small, well-named method so the rest of the code stays simple. Explain the *why* only if the decision is not obvious, following the comment rule below.
- Before finishing, reread your code as a newcomer would. If any line makes you pause, rename it or simplify it.

- **Names carry the meaning.** A name says exactly what the thing is or does: `isPlaylistEmpty()`, `formatDurationAsMinutesAndSeconds()`, not `check()` or `process()`. If you cannot name a function precisely, it is doing too much. Split it.
- **Small methods with one purpose.** Each method has one clear job, a clear input and a clear output. Prefer pure functions: the same input gives the same output and nothing else changes. Keep side effects (I/O, state changes) at the edges, in thin methods of their own.
- **A comment is a missing method.** Before writing a comment, extract the commented block into a method whose name says what the comment would have said. Then delete the comment.
- **No dead code, no commented-out code, no TODOs left behind.**
- **Follow the Kotlin conventions:** immutable by default (`val`, `data class`, read-only collections), expression bodies for one-liners, sealed types for closed sets of states, no `!!`.
- **Keep changes minimal.** Do not refactor or reformat code that is unrelated to the task. Mention it instead.

### Comments

Write a comment only when it explains a decision that the code cannot show: why this approach, why not the obvious one, a workaround for a platform bug, a constraint from outside the code. Never write a comment that says what the code does or restates the method name. KDoc on public API is not needed unless the contract is not clear from the signature.

## Testing

A **unit** is one small method with a clear purpose, input and output. Each unit gets its own tests. Because the methods are small and pure, the tests stay short and easy to read.

- **Write the unit test together with the method.** A new or changed method is not done until its tests exist and pass. Fixing a bug starts with a failing test that reproduces it.
- **Design for testability.** If a method is hard to test, the method is the problem. Extract the logic into a small pure method and test that.
- **One behavior per test.** Test names describe the behavior in plain words, in backticks: ``fun `formats 125 seconds as 2 minutes 5 seconds`()``. The name should make a failure understandable without opening the test.
- **Arrange, act, assert.** Separate the three parts with blank lines, no comments needed. Prefer one assertion per test, or a few that check one result.
- **No mocking libraries.** Use hand-written test doubles that implement the real interface (see `testing.md`). Put shared ones in a testing module once it exists.
- **Tests are deterministic.** No real network, no real clock, no sleeping. Inject a clock or dispatcher when needed and use Turbine for `Flow`.
- **Keep the tests as clean as the production code:** small helpers with good names for building test data, no copy-pasted setup.
- Unit tests go in `app/src/test/java/net/biodiscus/streammusic/` (mirror the package of the class under test). Instrumented and Compose UI tests go in `app/src/androidTest/`. UI tests are for wiring, not for logic that can be a unit test.

## Workflow

1. Read the relevant skill reference.
2. Write the smallest method that does the job, with a precise name.
3. Write its unit tests.
4. Run `./gradlew testDebugUnitTest` and make sure it is green before reporting the work as done.

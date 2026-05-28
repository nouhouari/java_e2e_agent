# Java E2E Agent — "Playwright for JavaFX"

A test automation framework that drives **unmodified JavaFX desktop applications** from Node.js/Cucumber tests. A Java agent is injected into the target JVM and exposes the scene graph over HTTP; a TypeScript driver provides a Playwright-like locator API.

```
Cucumber / Node.js tests
  → JavaFxDriver (lifecycle: launch JVM, connect, close)
    → JavaFxLocator (Playwright-like: auto-wait, selectors, actions)
      → HTTP (localhost)
        → Java Agent (Javalin server inside the target JVM)
          → Scene Graph + Robot
            → JavaFX Application
```

## Monorepo layout

| Directory | What it is |
|-----------|-----------|
| [`agent/`](./agent) | Java 21 / Gradle project. Produces `fxagent.jar` — a fat JAR loadable via `-javaagent`. Embeds a Javalin HTTP server, scene graph introspection, action executors. Also contains the `ShowcaseApp` test app. |
| [`driver/`](./driver) | TypeScript / npm package (`javafx-driver`). Exposes `JavaFxDriver` + `JavaFxLocator` with a Playwright-like API. Communicates with the agent over HTTP. |
| [`.github/workflows/`](./.github/workflows) | CI (build + Xvfb integration test) and release pipelines. |

## Requirements

- **Java 21+** with [JavaFX 21](https://openjfx.io/) (the Gradle build resolves JavaFX automatically)
- **Node.js 18+**
- **macOS, Linux, or Windows** with a display (or Xvfb on Linux)

## Quick start: running the demo

The demo launches a JavaFX showcase app (with every common control type) and drives it through ~30 interactions over ~50 seconds. The window stays open so you can watch every action happen in real time.

```bash
# 1. Build the agent (produces fxagent.jar + copies JavaFX libs)
cd agent
./gradlew prepareShowcase
cd ..

# 2. Install the driver
cd driver
npm install

# 3. Run the demo
npm run demo
```

You'll see:
- An info panel at the top narrating each step (`▶ Filling TextField with greeting...`)
- Tab-by-tab walkthrough: Text Inputs → Buttons → Selection → Display → Lists
- 20-row TableView scrolling between top/middle/bottom
- ColorPicker cycling through red → green → purple with a live swatch
- Screenshots saved to `driver/reports/demo/`

To slow it down, increase `STEP_PAUSE` at the top of `driver/test/demo.ts`. To speed it up, drop it. The full test suite (`npm test`) runs the same actions with no pauses in ~10 seconds.

## License

MIT — see [LICENSE](./LICENSE).

## Running the full test suite

```bash
cd driver
npm test
```

32 integration tests across 9 suites: text inputs, buttons, selection controls (including ColorPicker), display, lists/tables, selectors, visibility, waiting, and screenshots.

## Architecture highlights

- **Selectors** — `#id`, `.class`, `text=...`, `text~=...`, `TypeName`, `css=...`, `ref=...`, chainable with `>>`
- **Auto-waiting** — every action polls until the target is visible + enabled, then executes
- **FX thread safety** — all scene graph reads/writes bridged through `Platform.runLater()` via `CompletableFuture`
- **Two click strategies** — `ButtonBase.fire()` for buttons (most reliable), synthetic `MouseEvent` for everything else, with `{ strategy: 'robot' }` as an OS-level fallback
- **Programmatic selection** — `selectOption()` on ComboBox/ChoiceBox/ListView/TableView/ColorPicker uses the underlying `SelectionModel` directly (bypasses click fragility)
- **Per-element screenshots** — `fx.locator('#x').screenshot()` captures just that node, not the whole window
- **Multi-window** — `fx.screenshot('name', { windowIndex: N })` targets a specific window
- **Dependency isolation** — agent's Jetty/Jackson/SLF4J are shaded under `com.sicpa.fxagent.shaded.*` to avoid conflicts with the target app

## CI

Two GitHub Actions workflows:

- **`ci.yml`** — runs on push/PR: builds both projects in parallel, then runs the full integration test suite under Xvfb
- **`release.yml`** — on `v*` tag: publishes `fxagent.jar` to GitHub Releases + `javafx-driver` to npm

You can test the workflows locally with [`act`](https://github.com/nektos/act):

```bash
brew install act
act push -j build-agent   # run a single job
act push                  # run the full pipeline
```

The `.actrc` is preconfigured for Rancher Desktop / Docker Desktop on macOS.

## Subproject READMEs

- [agent/README.md](./agent/README.md) — Java agent: HTTP endpoints, selectors, action strategies, JVM args
- [driver/README.md](./driver/README.md) — Node driver: API reference, locator methods, error handling

## What it can automate

| Control | Action support |
|---------|----------------|
| `Button`, `Hyperlink` | `click()`, `doubleClick()` |
| `TextField`, `PasswordField`, `TextArea` | `fill()`, `clear()`, `text()` |
| `CheckBox`, `RadioButton`, `ToggleButton` | `click()` to toggle, `getAttribute('selected')` |
| `ComboBox`, `ChoiceBox` | `selectOption(value)` |
| `ColorPicker` | `selectOption('#FF0000')` or `selectOption('red')` |
| `ListView` | `selectOption('item')` or `selectOption('index=N')` |
| `TableView` | `selectOption('Bob')` (any column match) or `selectOption('index=N')` |
| `Slider`, `ProgressBar`, `ProgressIndicator` | Read via `getAttribute()` |
| `Label`, any `Labeled` | `text()`, `setText()` |
| `TabPane` tabs | `locator('text=TabName').click()` |
| All nodes | `isVisible()`, `isEnabled()`, `count()`, `waitFor()`, `screenshot()` |

**Not supported:** native `FileChooser` / `DirectoryChooser` dialogs (they're OS-level windows, not part of the scene graph — would require ByteBuddy interception).

# FxAgent

A Java agent that attaches to a running JavaFX application and exposes its scene graph over HTTP. Designed for test automation of **unmodified** JavaFX desktop applications.

## How it works

The agent is loaded into the target JVM via `-javaagent`. It waits for the JavaFX toolkit to initialize, then starts an embedded Javalin HTTP server inside the application process. The server exposes REST endpoints to query the scene graph, execute UI actions, and capture screenshots.

```
JavaFX Application JVM
├── Your App (unmodified)
└── FxAgent
    ├── Javalin HTTP Server (port 4567)
    ├── Scene Graph Introspection
    ├── Action Executor (click, fill, select...)
    └── Robot Fallback (OS-level input)
```

## Requirements

- Java 21+
- JavaFX 21+ (provided by the target application)

## Build

```bash
./gradlew shadowJar
```

Produces `build/libs/fxagent.jar` (~8MB) — a fat JAR with all dependencies shaded to avoid classpath conflicts.

## Usage

### Launch-time attachment (premain)

```bash
java -javaagent:fxagent.jar=port=4567 -jar myapp.jar
```

### Dynamic attachment (agentmain)

Use the JDK Attach API or a tool like `jcmd` to attach to a running JVM.

### Agent arguments

Passed as a comma-separated `key=value` string:

| Key | Default | Description |
|-----|---------|-------------|
| `port` | `4567` | HTTP server port |
| `toolkitTimeout` | `30000` | Max ms to wait for JavaFX toolkit to initialize |
| `actionTimeout` | `5000` | Default timeout for actions |

Arguments can also be set via system properties: `-Dfxagent.port=4567`, `-Dfxagent.toolkit.timeout=30000`, `-Dfxagent.action.timeout=5000`.

### Port detection

When the agent is ready, it prints to stdout:

```
FXAGENT_READY port=4567
```

The Node.js driver parses this line to discover the port automatically (useful when `port=0` for random port assignment).

## HTTP API

Base URL: `http://localhost:{port}/api/v1`

### Health

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/health` | Returns `{"status":"ok","version":"0.1.0"}` |
| `GET` | `/ready` | 200 if toolkit ready and windows showing, 503 otherwise |

### Windows

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/windows` | List all stages/windows |
| `GET` | `/windows/{index}` | Get a specific window by index |

### Elements

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/elements/query` | Find elements by selector |
| `GET` | `/elements/{handle}` | Get element state by handle |
| `GET` | `/elements/{handle}/tree` | Get subtree (query param: `depth`) |
| `POST` | `/elements/wait` | Wait for a selector to match a condition |
| `GET` | `/scene/tree` | Dump full scene graph (query param: `depth`) |

### Actions

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/actions` | Execute an action on a node |

Supported actions: `click`, `dblclick`, `rightclick`, `hover`, `fill`, `clear`, `select`, `focus`, `scroll`.

### Screenshots

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/screenshot` | Capture scene as base64 PNG |

## Selectors

| Syntax | Example | Matches |
|--------|---------|---------|
| `#id` | `#username` | `Node.getId()` |
| `.class` | `.primary-button` | `Node.getStyleClass()` |
| `text=...` | `text=Login` | Exact text on Labeled/TextInputControl |
| `text~=...` | `text~=Log` | Contains text |
| `TypeName` | `Button` | Class simple name (uppercase start) |
| `css=...` | `css=.panel > .item` | Native JavaFX CSS selector via `lookupAll()` |
| `ref=...` | `ref=ref-42` | Internal handle from a previous query |

Chain selectors with `>>` for parent-to-descendant narrowing:

```
#sidebar >> .menu-item >> text=Settings
```

## Action strategies

The agent uses a tiered approach for clicks:

1. **`ButtonBase.fire()`** for buttons — most reliable, works even if the window is obscured
2. **Synthetic MouseEvent** for other nodes — fires PRESSED/RELEASED/CLICKED sequence on the scene graph
3. **Robot fallback** (`options.strategy: "robot"`) — real OS-level mouse input via `javafx.scene.robot.Robot`

## Architecture

All scene graph operations run on the **JavaFX Application Thread** via `Platform.runLater()`, bridged to Javalin's Jetty threads using `CompletableFuture`. The HTTP handlers use `ctx.future()` for non-blocking async responses.

Dependencies are relocated under `com.sicpa.fxagent.shaded.*` to avoid conflicts with libraries the target application may use (Jetty, Jackson, SLF4J). JavaFX classes are **not** bundled — they come from the target JVM.

Nodes are tracked across requests using opaque handles (`ref-1`, `ref-2`, ...) backed by `WeakReference<Node>`, preventing memory leaks when nodes are removed from the scene graph.

## Example

```bash
# Start app with agent
java -javaagent:build/libs/fxagent.jar -jar myapp.jar

# Query elements
curl -s http://localhost:4567/api/v1/elements/query \
  -H 'Content-Type: application/json' \
  -d '{"selector": "#username"}' | jq .

# Click a button
curl -s http://localhost:4567/api/v1/actions \
  -H 'Content-Type: application/json' \
  -d '{"selector": "#loginButton", "action": "click"}' | jq .

# Take a screenshot
curl -s http://localhost:4567/api/v1/screenshot \
  -H 'Content-Type: application/json' \
  -d '{}' | jq -r .data | base64 -d > screenshot.png
```

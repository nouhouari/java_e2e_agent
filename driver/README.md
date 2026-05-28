# javafx-driver

A Playwright-like Node.js driver for automating JavaFX desktop applications. Communicates with a Java agent running inside the target JVM over HTTP.

## How it works

```
Your test code
  → JavaFxDriver (manages JVM lifecycle)
    → JavaFxLocator (Playwright-like API with auto-waiting)
      → JavaFxClient (HTTP)
        → FxAgent (Java agent inside the JavaFX app)
          → Scene Graph / Robot
```

The driver spawns the JavaFX application as a child process with the agent JAR attached via `-javaagent`. Once the agent reports ready, the driver communicates with it over a local HTTP connection.

## Requirements

- Node.js 18+
- Java 21+ with JavaFX
- The [FxAgent](../agent/) JAR (build with `cd ../agent && ./gradlew shadowJar`)

## Install

```bash
npm install javafx-driver
```

Or link locally during development:

```bash
npm install ../java_e2e_agent/driver
```

## Quick start

```typescript
import { JavaFxDriver } from 'javafx-driver';

const fx = new JavaFxDriver({
  agentJar: '../agent/build/libs/fxagent.jar',
});

await fx.launch({
  app: 'com.example.MyApp',
  classpath: './build/libs/myapp.jar',
});

// Fill a text field
await fx.locator('#username').fill('admin');
await fx.locator('#password').fill('secret');

// Click a button
await fx.locator('#loginButton').click();

// Wait for a view to appear
await fx.locator('#dashboard').waitFor({ state: 'visible', timeout: 10000 });

// Read text
const welcome = await fx.locator('#welcomeLabel').text();

// Take a screenshot
await fx.screenshot('after-login');

// Clean up
await fx.close();
```

## API

### `new JavaFxDriver(config)`

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `agentJar` | `string` | *required* | Path to the FxAgent JAR |
| `javaBin` | `string` | `'java'` | Java binary path |
| `agentPort` | `number` | `0` | Agent HTTP port (0 = random, detected from stdout) |
| `agentHost` | `string` | `'127.0.0.1'` | Agent bind host |
| `defaultTimeoutMs` | `number` | `5000` | Default timeout for locator actions |
| `pollIntervalMs` | `number` | `100` | Polling interval for auto-wait |
| `screenshotDir` | `string` | `'reports/screenshots'` | Screenshot output directory |
| `jvmArgs` | `string[]` | `[]` | Additional JVM arguments |

### `fx.launch(options)`

Spawns the JavaFX application with the agent attached.

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `app` | `string` | *required* | Fully qualified main class |
| `classpath` | `string \| string[]` | — | Application classpath |
| `modulePath` | `string \| string[]` | — | Module path |
| `jvmArgs` | `string[]` | — | Extra JVM args for this launch |
| `env` | `Record<string, string>` | — | Environment variables |
| `cwd` | `string` | — | Working directory |
| `readyTimeoutMs` | `number` | `15000` | Max ms to wait for agent ready |

### `fx.connect(host, port)`

Connect to an already-running agent (e.g., after dynamic attach).

### `fx.locator(selector)`

Returns a `JavaFxLocator` — a lazy handle that resolves when an action is performed.

### `fx.screenshot(name)`

Captures a screenshot and saves to `{screenshotDir}/{name}.png`. Returns the PNG `Buffer`.

### `fx.close()`

Shuts down the JavaFX application process.

## Locator API

Locators are lazy — creating one does no network calls. Actions trigger auto-waiting: the driver polls the agent until the element is actionable, then performs the action.

### Actions

All actions auto-wait for the element to be **visible and enabled**.

```typescript
await fx.locator('#btn').click();
await fx.locator('#btn').doubleClick();
await fx.locator('#input').fill('text');
await fx.locator('#input').clear();
await fx.locator('#combo').selectOption('value');
await fx.locator('#field').focus();
```

### Queries

Queries auto-wait for the element to **exist**.

```typescript
const text = await fx.locator('#label').text();
const info = await fx.locator('#node').nodeInfo();
const value = await fx.locator('#checkbox').getAttribute('selected');
```

### State checks

No waiting — single attempt, returns boolean.

```typescript
const visible = await fx.locator('#dialog').isVisible();
const enabled = await fx.locator('#btn').isEnabled();
const count = await fx.locator('.list-item').count();
```

### Waiting

```typescript
await fx.locator('#dialog').waitFor({ state: 'visible', timeout: 10000 });
await fx.locator('#spinner').waitFor({ state: 'hidden' });
await fx.locator('#btn').waitFor({ state: 'enabled' });
```

### Chaining

Narrow searches to a subtree:

```typescript
await fx.locator('#sidebar').locator('.menu-item').click();
```

## Selectors

| Syntax | Example | Matches |
|--------|---------|---------|
| `#id` | `#username` | Node by `fx:id` |
| `.class` | `.primary-button` | Node by style class |
| `text=...` | `text=Login` | Exact text match |
| `text~=...` | `text~=Log` | Contains text |
| `TypeName` | `Button` | Class name (uppercase start) |
| `css=...` | `css=.panel > .item` | Native JavaFX CSS |

Chain with `>>`: `#panel >> .item >> text=Settings`

## Error handling

```typescript
import { TimeoutError, ActionError, ConnectionError } from 'javafx-driver';

try {
  await fx.locator('#missing').click({ timeout: 3000 });
} catch (e) {
  if (e instanceof TimeoutError) {
    console.log(e.selector);    // '#missing'
    console.log(e.condition);   // 'visible and enabled'
    console.log(e.timeoutMs);   // 3000
  }
}
```

| Error | When |
|-------|------|
| `ConnectionError` | Agent not reachable |
| `TimeoutError` | Element not found/actionable within timeout |
| `ActionError` | Action failed (e.g., fill on a non-text node) |
| `ProtocolError` | Agent returned an HTTP error |
| `LaunchError` | JVM process failed to start |

## Conductor integration

This driver integrates with the [Conductor](https://github.com/user/conductor) multi-platform test framework as a `@desktop` driver:

```typescript
// In Cucumber step definitions
Given('the app is running', async function (this: ConductorWorld) {
  await this.fx.launch({
    app: 'com.example.MyApp',
    classpath: './build/libs/myapp.jar',
  });
});

When('I login as {string}', async function (this: ConductorWorld, user: string) {
  await this.fx.locator('#username').fill(user);
  await this.fx.locator('#password').fill('secret');
  await this.fx.locator('#loginButton').click();
});

Then('the dashboard should be visible', async function (this: ConductorWorld) {
  await this.fx.locator('#dashboard').waitFor({ state: 'visible' });
});
```

Tag scenarios with `@desktop` for automatic teardown and failure screenshots.

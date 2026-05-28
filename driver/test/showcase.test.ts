import { describe, it, before, after } from 'node:test';
import assert from 'node:assert';
import * as path from 'path';
import * as fs from 'fs';
import { JavaFxDriver } from '../src';

const AGENT_DIR = path.resolve(__dirname, '../../agent');
const AGENT_JAR = path.join(AGENT_DIR, 'build/libs/fxagent.jar');
const JAVAFX_LIBS = path.join(AGENT_DIR, 'build/javafx-libs');
const TEST_CLASSES = path.join(AGENT_DIR, 'build/classes/java/test');

let fx: JavaFxDriver;

describe('ShowcaseApp Integration Tests', () => {
  before(async () => {
    assert.ok(fs.existsSync(AGENT_JAR), `Agent JAR not found at ${AGENT_JAR}. Run: cd agent && ./gradlew prepareShowcase`);
    assert.ok(fs.existsSync(JAVAFX_LIBS), `JavaFX libs not found at ${JAVAFX_LIBS}. Run: cd agent && ./gradlew prepareShowcase`);

    fx = new JavaFxDriver({
      agentJar: AGENT_JAR,
      agentPort: 0,
      defaultTimeoutMs: 10_000,
      screenshotDir: path.resolve(__dirname, '../reports/screenshots'),
    });

    await fx.launch({
      app: 'com.sicpa.fxagent.testapp.ShowcaseApp',
      classpath: TEST_CLASSES,
      jvmArgs: [
        '--module-path', JAVAFX_LIBS,
        '--add-modules', 'javafx.controls,javafx.swing',
      ],
      readyTimeoutMs: 30_000,
    });
  });

  after(async () => {
    if (fx?.isLaunched) {
      await fx.close();
    }
  });

  // ---- Text Inputs ----

  describe('Text Inputs', () => {
    it('should fill a TextField and verify echo', async () => {
      await fx.locator('#textField').fill('hello world');
      const echo = await fx.locator('#textFieldEcho').text();
      assert.strictEqual(echo, 'hello world');
    });

    it('should clear a TextField', async () => {
      await fx.locator('#textField').fill('to be cleared');
      await fx.locator('#textField').clear();
      const echo = await fx.locator('#textFieldEcho').text();
      assert.strictEqual(echo, '');
    });

    it('should fill a PasswordField', async () => {
      await fx.locator('#passwordField').fill('secret123');
      const echo = await fx.locator('#passwordEcho').text();
      assert.strictEqual(echo, 'secret123');
    });

    it('should fill a TextArea', async () => {
      await fx.locator('#textArea').fill('line one');
      const echo = await fx.locator('#textAreaEcho').text();
      assert.strictEqual(echo, 'line one');
    });
  });

  // ---- Buttons ----

  describe('Buttons', () => {
    it('should click a Button and verify status', async () => {
      await fx.locator('#button').click();
      const status = await fx.locator('#buttonStatus').text();
      assert.strictEqual(status, 'Clicked');
    });

    it('should increment counter on multiple clicks', async () => {
      await fx.locator('#counterButton').click();
      await fx.locator('#counterButton').click();
      await fx.locator('#counterButton').click();
      const label = await fx.locator('#counterLabel').text();
      assert.strictEqual(label, 'Count: 3');
    });

    it('should toggle a ToggleButton on and off', async () => {
      await fx.locator('#toggleButton').click();
      let status = await fx.locator('#toggleStatus').text();
      assert.strictEqual(status, 'ON');

      await fx.locator('#toggleButton').click();
      status = await fx.locator('#toggleStatus').text();
      assert.strictEqual(status, 'OFF');
    });

    it('should click a Hyperlink', async () => {
      await fx.locator('#hyperlink').click();
      const status = await fx.locator('#hyperlinkStatus').text();
      assert.strictEqual(status, 'Visited');
    });
  });

  // ---- Selection Controls ----

  describe('Selection Controls', () => {
    it('should toggle a CheckBox', async () => {
      await fx.locator('#checkBox').click();
      let status = await fx.locator('#checkBoxStatus').text();
      assert.strictEqual(status, 'Checked');

      await fx.locator('#checkBox').click();
      status = await fx.locator('#checkBoxStatus').text();
      assert.strictEqual(status, 'Unchecked');
    });

    it('should select a RadioButton', async () => {
      await fx.locator('#radioOption2').click();
      const status = await fx.locator('#radioStatus').text();
      assert.strictEqual(status, 'Option B');
    });

    it('should select a different RadioButton', async () => {
      await fx.locator('#radioOption3').click();
      const status = await fx.locator('#radioStatus').text();
      assert.strictEqual(status, 'Option C');
    });

    it('should select a ComboBox value', async () => {
      await fx.locator('#comboBox').selectOption('Cherry');
      const status = await fx.locator('#comboStatus').text();
      assert.strictEqual(status, 'Cherry');
    });

    it('should select a ChoiceBox value', async () => {
      await fx.locator('#choiceBox').selectOption('Large');
      const status = await fx.locator('#choiceStatus').text();
      assert.strictEqual(status, 'Large');
    });
  });

  // ---- Display & Range ----

  describe('Display & Range', () => {
    it('should read a Label text', async () => {
      const text = await fx.locator('#displayLabel').text();
      assert.strictEqual(text, 'Hello, JavaFX!');
    });

    it('should read ProgressBar progress', async () => {
      const info = await fx.locator('#progressBar').nodeInfo();
      assert.ok(info.properties, 'ProgressBar should have properties');
      assert.strictEqual(info.properties!['progress'], 0.65);
    });

    it('should verify Slider is visible', async () => {
      const visible = await fx.locator('#slider').isVisible();
      assert.ok(visible, 'Slider should be visible');
    });

    it('should read Slider value via echo label', async () => {
      const text = await fx.locator('#sliderValue').text();
      assert.strictEqual(text, '50');
    });
  });

  // ---- Lists & Tables ----

  describe('Lists & Tables', () => {
    it('should read ListView item count', async () => {
      const info = await fx.locator('#listView').nodeInfo();
      assert.ok(info.properties, 'ListView should have properties');
      assert.strictEqual(info.properties!['itemCount'], 5);
    });

    it('should read TableView row count', async () => {
      const info = await fx.locator('#tableView').nodeInfo();
      assert.ok(info.properties, 'TableView should have properties');
      assert.strictEqual(info.properties!['rowCount'], 4);
      assert.strictEqual(info.properties!['columnCount'], 3);
    });
  });

  // ---- Selectors ----

  describe('Selectors', () => {
    it('should find elements by style class', async () => {
      const count = await fx.locator('.section-title').count();
      assert.ok(count > 0, 'Should find elements with .section-title class');
    });

    it('should find elements by type name', async () => {
      const count = await fx.locator('Button').count();
      assert.ok(count >= 2, 'Should find at least 2 Button elements');
    });

    it('should find elements by text selector', async () => {
      const visible = await fx.locator('text=Hello, JavaFX!').isVisible();
      assert.ok(visible, 'Should find label by text');
    });
  });

  // ---- Visibility & State ----

  describe('Visibility & State', () => {
    it('should check element visibility', async () => {
      const visible = await fx.locator('#button').isVisible();
      assert.ok(visible);
    });

    it('should check element enabled state', async () => {
      const enabled = await fx.locator('#button').isEnabled();
      assert.ok(enabled);
    });

    it('should wait for an element to be visible', async () => {
      await fx.locator('#buttonStatus').waitFor({ state: 'visible', timeout: 5000 });
    });
  });

  // ---- Screenshots ----

  describe('Screenshots', () => {
    it('should capture a full window screenshot', async () => {
      const buffer = await fx.screenshot('showcase');
      assert.ok(buffer.length > 0, 'Screenshot buffer should not be empty');

      const screenshotPath = path.resolve(__dirname, '../reports/screenshots/showcase.png');
      assert.ok(fs.existsSync(screenshotPath), 'Screenshot file should exist');
    });

    it('should capture a per-element screenshot via locator', async () => {
      const buffer = await fx.locator('#button').screenshot();
      assert.ok(buffer.length > 0, 'Element screenshot buffer should not be empty');
    });

    it('should capture a per-element screenshot via selector option', async () => {
      const buffer = await fx.screenshot('button-element', { selector: '#button' });
      assert.ok(buffer.length > 0, 'Element screenshot buffer should not be empty');

      const screenshotPath = path.resolve(__dirname, '../reports/screenshots/button-element.png');
      assert.ok(fs.existsSync(screenshotPath), 'Element screenshot file should exist');
    });

    it('should capture screenshot of a specific window by index', async () => {
      const buffer = await fx.screenshot('window-0', { windowIndex: 0 });
      assert.ok(buffer.length > 0, 'Window screenshot buffer should not be empty');
    });

    it('element screenshot should be smaller than full window', async () => {
      const fullBuffer = await fx.screenshot('full-compare');
      const elementBuffer = await fx.locator('#button').screenshot();
      assert.ok(
        elementBuffer.length < fullBuffer.length,
        `Element screenshot (${elementBuffer.length}) should be smaller than full window (${fullBuffer.length})`,
      );
    });
  });
});

import * as path from 'path';
import * as fs from 'fs';
import { JavaFxDriver } from '../src';

const AGENT_DIR = path.resolve(__dirname, '../../agent');
const AGENT_JAR = path.join(AGENT_DIR, 'build/libs/fxagent.jar');
const JAVAFX_LIBS = path.join(AGENT_DIR, 'build/javafx-libs');
const TEST_CLASSES = path.join(AGENT_DIR, 'build/classes/java/test');

const STEP_PAUSE = 1500;

async function sleep(ms: number) {
  return new Promise(r => setTimeout(r, ms));
}

async function main() {
  const fx = new JavaFxDriver({
    agentJar: AGENT_JAR,
    agentPort: 0,
    defaultTimeoutMs: 10_000,
    screenshotDir: path.resolve(__dirname, '../reports/demo'),
  });

  // Helper that narrates each step in both the terminal AND the in-app info panel
  async function step(description: string, action: () => Promise<void>): Promise<void> {
    console.log(`  → ${description}`);
    await fx.locator('#demoStatus').setText(description);
    await action();
    await sleep(STEP_PAUSE);
  }

  console.log('Launching ShowcaseApp...');
  await fx.launch({
    app: 'com.sicpa.fxagent.testapp.ShowcaseApp',
    classpath: TEST_CLASSES,
    jvmArgs: ['--module-path', JAVAFX_LIBS, '--add-modules', 'javafx.controls,javafx.swing'],
    readyTimeoutMs: 30_000,
  });
  await sleep(2000);

  console.log('\n=== Tab 1: Text Inputs ===');
  await fx.screenshot('01-initial');
  await step('Filling TextField with greeting...',
    () => fx.locator('#textField').fill('Hello from Node.js!'));
  await step('Filling PasswordField...',
    () => fx.locator('#passwordField').fill('s3cret'));
  await step('Filling TextArea with multi-line content...',
    () => fx.locator('#textArea').fill('Multi-line\nautomation\nworks great!'));
  await fx.screenshot('02-text-filled');

  console.log('\n=== Tab 2: Buttons ===');
  await step('Switching to Buttons tab...',
    () => fx.locator('text=Buttons').click());
  await step('Clicking the basic Button...',
    () => fx.locator('#button').click());
  for (let i = 1; i <= 3; i++) {
    await step(`Clicking counter button (${i}/3)...`,
      () => fx.locator('#counterButton').click());
  }
  await step('Toggling the ToggleButton ON...',
    () => fx.locator('#toggleButton').click());
  await step('Clicking the Hyperlink...',
    () => fx.locator('#hyperlink').click());
  await fx.screenshot('03-buttons-clicked');

  console.log('\n=== Tab 3: Selection ===');
  await step('Switching to Selection tab...',
    () => fx.locator('text=Selection').click());
  await step('Checking the CheckBox...',
    () => fx.locator('#checkBox').click());
  await step('Selecting Radio Option B...',
    () => fx.locator('#radioOption2').click());
  await step('Picking "Cherry" from ComboBox...',
    () => fx.locator('#comboBox').selectOption('Cherry'));
  await step('Picking "Large" from ChoiceBox...',
    () => fx.locator('#choiceBox').selectOption('Large'));
  await step('Setting ColorPicker to red (#FF0000)...',
    () => fx.locator('#colorPicker').selectOption('#FF0000'));
  await step('Setting ColorPicker to green (#10B981)...',
    () => fx.locator('#colorPicker').selectOption('#10B981'));
  await step('Setting ColorPicker to purple (#8B5CF6)...',
    () => fx.locator('#colorPicker').selectOption('#8B5CF6'));
  const color = await fx.locator('#colorPicker').getAttribute('value');
  console.log(`     ColorPicker value: ${color}`);
  await fx.screenshot('04-selection-made');

  console.log('\n=== Tab 4: Display & Range ===');
  await step('Switching to Display tab (read-only controls)...',
    () => fx.locator('text=Display').click());
  await sleep(STEP_PAUSE);
  await fx.screenshot('05-display-tab');

  console.log('\n=== Tab 5: Lists & Tables ===');
  await step('Switching to Lists tab (20 table rows, scroll required)...',
    () => fx.locator('text=Lists').click());
  await fx.screenshot('06-lists-tab');

  await step('Selecting "Item 3" in ListView (matches by toString)...',
    () => fx.locator('#listView').selectOption('Item 3'));
  const listSel = await fx.locator('#listSelectionStatus').text();
  console.log(`     status: "${listSel}"`);

  await step('Selecting row by name → "Bob" (table scrolls if needed)...',
    () => fx.locator('#tableView').selectOption('Bob'));
  console.log(`     status: "${await fx.locator('#tableSelectionStatus').text()}"`);

  await step('Scrolling to last row → "Tara" (index 19, bottom of table)...',
    () => fx.locator('#tableView').selectOption('index=19'));
  console.log(`     status: "${await fx.locator('#tableSelectionStatus').text()}"`);
  await fx.screenshot('07-table-scrolled-bottom');

  await step('Selecting by city → "Istanbul" (matches Samir, mid-table)...',
    () => fx.locator('#tableView').selectOption('Istanbul'));
  console.log(`     status: "${await fx.locator('#tableSelectionStatus').text()}"`);
  await fx.screenshot('08-table-scrolled-middle');

  await step('Scrolling back to top → index=0 (Alice)...',
    () => fx.locator('#tableView').selectOption('index=0'));
  console.log(`     status: "${await fx.locator('#tableSelectionStatus').text()}"`);
  await fx.screenshot('09-table-scrolled-top');

  await step('Toggling the "Verbose" checkbox in the info panel...',
    () => fx.locator('#verboseMode').click());
  await step('Toggling it back on...',
    () => fx.locator('#verboseMode').click());

  console.log('\n=== Tab 6: Dialogs ===');
  await step('Switching to Dialogs tab...',
    () => fx.locator('text=Dialogs').click());

  await step('Opening a Confirmation alert...',
    () => fx.locator('#showAlertBtn').click());
  await fx.locator('#confirmationDialog').waitFor({ state: 'visible', timeout: 5000 });
  await fx.screenshot('11-confirmation-dialog');
  await step('Clicking OK on the confirmation...',
    () => fx.locator('text=OK').click());
  console.log(`     status: "${await fx.locator('#alertResult').text()}"`);

  await step('Opening the same dialog again...',
    () => fx.locator('#showAlertBtn').click());
  await fx.locator('#confirmationDialog').waitFor({ state: 'visible', timeout: 5000 });
  await step('Clicking Cancel this time...',
    () => fx.locator('text=Cancel').click());
  console.log(`     status: "${await fx.locator('#alertResult').text()}"`);

  await step('Opening a TextInputDialog...',
    () => fx.locator('#showInputBtn').click());
  await fx.locator('#inputDialog').waitFor({ state: 'visible', timeout: 5000 });
  await step('Filling the input field with "Alice"...',
    () => fx.locator('#inputDialogField').fill('Alice'));
  await fx.screenshot('12-input-dialog');
  await step('Clicking OK to confirm input...',
    () => fx.locator('text=OK').click());
  console.log(`     status: "${await fx.locator('#inputResult').text()}"`);

  await step('Opening an Information alert...',
    () => fx.locator('#showInfoBtn').click());
  await fx.locator('#infoDialog').waitFor({ state: 'visible', timeout: 5000 });
  await step('Acknowledging the info dialog...',
    () => fx.locator('text=OK').click());
  console.log(`     status: "${await fx.locator('#infoResult').text()}"`);

  await fx.locator('#demoStatus').setText('Demo complete!');
  console.log('\nCapturing per-element screenshot of TableView...');
  const tableBuf = await fx.locator('#tableView').screenshot();
  fs.writeFileSync(
    path.resolve(__dirname, '../reports/demo/10-tableview-element.png'),
    tableBuf,
  );

  await sleep(2000);
  console.log('\nClosing app...');
  await fx.close();
  console.log('Demo complete! Screenshots saved to driver/reports/demo/');
}

main().catch((e) => {
  console.error('Demo failed:', e);
  process.exit(1);
});

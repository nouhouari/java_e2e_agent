import { JavaFxClient } from './JavaFxClient';
import { waitUntil } from './wait';
import { ActionError } from './errors';
import type {
  AgentNodeInfo, LocatorOptions, FillOptions, ClickOptions,
  SelectOptions, WaitForOptions,
} from './types';

export class JavaFxLocator {
  private readonly client: JavaFxClient;
  private readonly selector: string;
  private readonly defaultTimeoutMs: number;
  private readonly pollIntervalMs: number;
  private readonly parentSelector?: string;

  constructor(
    client: JavaFxClient,
    selector: string,
    defaultTimeoutMs: number,
    pollIntervalMs: number,
    parentSelector?: string,
  ) {
    this.client = client;
    this.selector = selector;
    this.defaultTimeoutMs = defaultTimeoutMs;
    this.pollIntervalMs = pollIntervalMs;
    this.parentSelector = parentSelector;
  }

  // ---- Chaining ----

  locator(childSelector: string, _opts?: LocatorOptions): JavaFxLocator {
    return new JavaFxLocator(
      this.client,
      childSelector,
      this.defaultTimeoutMs,
      this.pollIntervalMs,
      this.resolvedSelector(),
    );
  }

  // ---- Actions (auto-wait for visible + enabled) ----

  async click(opts?: ClickOptions): Promise<void> {
    const timeout = opts?.timeout ?? this.defaultTimeoutMs;
    await this.waitForActionable(timeout);
    const sel = this.resolvedSelector();
    const response = await this.client.performAction(sel, 'click', {
      clickCount: opts?.clickCount ?? 1,
    });
    if (!response.success) {
      throw new ActionError(sel, 'click', response.message);
    }
  }

  async doubleClick(opts?: ClickOptions): Promise<void> {
    return this.click({ ...opts, clickCount: 2 });
  }

  async fill(text: string, opts?: FillOptions): Promise<void> {
    const timeout = opts?.timeout ?? this.defaultTimeoutMs;
    const clear = opts?.clear ?? true;
    await this.waitForActionable(timeout);
    const sel = this.resolvedSelector();

    if (clear) {
      const clearResponse = await this.client.performAction(sel, 'clear');
      if (!clearResponse.success) {
        throw new ActionError(sel, 'clear', clearResponse.message);
      }
    }

    const response = await this.client.performAction(sel, 'fill', { value: text });
    if (!response.success) {
      throw new ActionError(sel, 'fill', response.message);
    }
  }

  async clear(opts?: { timeout?: number }): Promise<void> {
    const timeout = opts?.timeout ?? this.defaultTimeoutMs;
    await this.waitForActionable(timeout);
    const sel = this.resolvedSelector();
    const response = await this.client.performAction(sel, 'clear');
    if (!response.success) {
      throw new ActionError(sel, 'clear', response.message);
    }
  }

  async selectOption(value: string, opts?: SelectOptions): Promise<void> {
    const timeout = opts?.timeout ?? this.defaultTimeoutMs;
    await this.waitForActionable(timeout);
    const sel = this.resolvedSelector();
    const response = await this.client.performAction(sel, 'select', { value });
    if (!response.success) {
      throw new ActionError(sel, 'select', response.message);
    }
  }

  async focus(opts?: { timeout?: number }): Promise<void> {
    const timeout = opts?.timeout ?? this.defaultTimeoutMs;
    await this.waitForActionable(timeout);
    const sel = this.resolvedSelector();
    const response = await this.client.performAction(sel, 'focus');
    if (!response.success) {
      throw new ActionError(sel, 'focus', response.message);
    }
  }

  // ---- Queries (auto-wait for existence) ----

  async text(opts?: { timeout?: number }): Promise<string> {
    const timeout = opts?.timeout ?? this.defaultTimeoutMs;
    const node = await this.waitForExists(timeout);
    return node.text ?? '';
  }

  async getAttribute(name: string, opts?: { timeout?: number }): Promise<unknown> {
    const timeout = opts?.timeout ?? this.defaultTimeoutMs;
    const node = await this.waitForExists(timeout);
    return node.properties?.[name] ?? null;
  }

  async nodeInfo(opts?: { timeout?: number }): Promise<AgentNodeInfo> {
    const timeout = opts?.timeout ?? this.defaultTimeoutMs;
    return this.waitForExists(timeout);
  }

  // ---- Screenshots ----

  async screenshot(opts?: { timeout?: number }): Promise<Buffer> {
    const timeout = opts?.timeout ?? this.defaultTimeoutMs;
    await this.waitForExists(timeout);
    const response = await this.client.captureScreenshot({ selector: this.resolvedSelector() });
    return Buffer.from(response.data, 'base64');
  }

  // ---- State queries (no waiting) ----

  async isVisible(): Promise<boolean> {
    try {
      const response = await this.client.queryNode(this.resolvedSelector());
      return response.elements.length > 0 && response.elements[0].visible;
    } catch {
      return false;
    }
  }

  async isEnabled(): Promise<boolean> {
    try {
      const response = await this.client.queryNode(this.resolvedSelector());
      return response.elements.length > 0 && !response.elements[0].disabled;
    } catch {
      return false;
    }
  }

  async count(): Promise<number> {
    const response = await this.client.queryAll(this.resolvedSelector());
    return response.elements.length;
  }

  // ---- Waiting ----

  async waitFor(opts?: WaitForOptions): Promise<void> {
    const state = opts?.state ?? 'visible';
    const timeout = opts?.timeout ?? this.defaultTimeoutMs;

    switch (state) {
      case 'visible':
        await this.waitForVisible(timeout);
        break;
      case 'hidden':
        await this.waitForHidden(timeout);
        break;
      case 'enabled':
        await this.waitForActionable(timeout);
        break;
      case 'disabled':
        await this.waitForDisabled(timeout);
        break;
    }
  }

  // ---- Private helpers ----

  private resolvedSelector(): string {
    if (this.parentSelector) {
      return `${this.parentSelector} >> ${this.selector}`;
    }
    return this.selector;
  }

  private async waitForExists(timeoutMs: number): Promise<AgentNodeInfo> {
    const sel = this.resolvedSelector();
    return waitUntil(
      async () => {
        const response = await this.client.queryNode(sel);
        return response.elements.length > 0 ? response.elements[0] : null;
      },
      { timeoutMs, pollIntervalMs: this.pollIntervalMs, selector: sel, condition: 'attached' },
    );
  }

  private async waitForVisible(timeoutMs: number): Promise<AgentNodeInfo> {
    const sel = this.resolvedSelector();
    return waitUntil(
      async () => {
        const response = await this.client.queryNode(sel);
        if (response.elements.length > 0 && response.elements[0].visible) {
          return response.elements[0];
        }
        return null;
      },
      { timeoutMs, pollIntervalMs: this.pollIntervalMs, selector: sel, condition: 'visible' },
    );
  }

  private async waitForActionable(timeoutMs: number): Promise<AgentNodeInfo> {
    const sel = this.resolvedSelector();
    return waitUntil(
      async () => {
        const response = await this.client.queryNode(sel);
        if (
          response.elements.length > 0 &&
          response.elements[0].visible &&
          !response.elements[0].disabled
        ) {
          return response.elements[0];
        }
        return null;
      },
      { timeoutMs, pollIntervalMs: this.pollIntervalMs, selector: sel, condition: 'visible and enabled' },
    );
  }

  private async waitForHidden(timeoutMs: number): Promise<void> {
    const sel = this.resolvedSelector();
    await waitUntil(
      async () => {
        const response = await this.client.queryNode(sel);
        if (response.elements.length === 0 || !response.elements[0].visible) {
          return true;
        }
        return null;
      },
      { timeoutMs, pollIntervalMs: this.pollIntervalMs, selector: sel, condition: 'hidden' },
    );
  }

  private async waitForDisabled(timeoutMs: number): Promise<void> {
    const sel = this.resolvedSelector();
    await waitUntil(
      async () => {
        const response = await this.client.queryNode(sel);
        if (response.elements.length > 0 && response.elements[0].disabled) {
          return true;
        }
        return null;
      },
      { timeoutMs, pollIntervalMs: this.pollIntervalMs, selector: sel, condition: 'disabled' },
    );
  }
}

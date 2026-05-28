import { ChildProcess, spawn } from 'child_process';
import * as fs from 'fs';
import * as path from 'path';
import { JavaFxClient } from './JavaFxClient';
import { JavaFxLocator } from './JavaFxLocator';
import { ConnectionError, LaunchError } from './errors';
import { waitUntil } from './wait';
import type { JavaFxConfig, JavaFxLaunchOptions, LocatorOptions, ScreenshotOptions } from './types';

interface ResolvedConfig {
  javaBin: string;
  agentJar: string;
  agentPort: number;
  agentHost: string;
  defaultTimeoutMs: number;
  pollIntervalMs: number;
  screenshotDir: string;
  jvmArgs: string[];
}

export class JavaFxDriver {
  private readonly _config: ResolvedConfig;
  private _client: JavaFxClient | null = null;
  private _process: ChildProcess | null = null;
  private _port = 0;
  private _exitHandler: (() => void) | null = null;

  constructor(config: JavaFxConfig) {
    this._config = {
      javaBin: config.javaBin ?? 'java',
      agentJar: config.agentJar,
      agentPort: config.agentPort ?? 0,
      agentHost: config.agentHost ?? '127.0.0.1',
      defaultTimeoutMs: config.defaultTimeoutMs ?? 5000,
      pollIntervalMs: config.pollIntervalMs ?? 100,
      screenshotDir: config.screenshotDir ?? 'reports/screenshots',
      jvmArgs: config.jvmArgs ?? [],
    };
  }

  async launch(opts: JavaFxLaunchOptions): Promise<void> {
    const agentJar = path.resolve(opts.agentJar ?? this._config.agentJar);
    if (!fs.existsSync(agentJar)) {
      throw new LaunchError(`Agent JAR not found: ${agentJar}`, null);
    }

    const port = this._config.agentPort;
    const agentArg = `-javaagent:${agentJar}=port=${port}`;

    const args: string[] = [
      ...this._config.jvmArgs,
      ...(opts.jvmArgs ?? []),
      agentArg,
    ];

    if (opts.modulePath) {
      const mp = Array.isArray(opts.modulePath)
        ? opts.modulePath.join(path.delimiter)
        : opts.modulePath;
      args.push('--module-path', mp);
    }

    if (opts.classpath) {
      const cp = Array.isArray(opts.classpath)
        ? opts.classpath.join(path.delimiter)
        : opts.classpath;
      args.push('-cp', cp);
    }

    args.push(opts.app);

    const env = { ...process.env, ...(opts.env ?? {}) };
    this._process = spawn(this._config.javaBin, args, {
      env,
      cwd: opts.cwd,
      stdio: ['ignore', 'pipe', 'pipe'],
    });

    let stderr = '';
    this._process.stderr?.on('data', (chunk: Buffer) => {
      stderr += chunk.toString();
    });

    const readyTimeoutMs = opts.readyTimeoutMs ?? 15_000;

    const detectedPort = await this.detectPort(readyTimeoutMs, stderr);
    this._port = detectedPort;
    this._client = new JavaFxClient(this._config.agentHost, this._port);

    await this.waitForReady(readyTimeoutMs, stderr);

    this._exitHandler = () => this._cleanup();
    process.on('exit', this._exitHandler);
  }

  async connect(host: string, port: number): Promise<void> {
    this._port = port;
    this._client = new JavaFxClient(host, port);

    await waitUntil(
      async () => {
        const ready = await this._client!.isReady();
        return ready ? true : null;
      },
      {
        timeoutMs: 15_000,
        pollIntervalMs: 500,
        selector: 'agent',
        condition: 'ready',
      },
    );
  }

  async close(): Promise<void> {
    if (this._process) {
      this._process.kill('SIGTERM');

      await new Promise<void>((resolve) => {
        const timer = setTimeout(() => {
          this._process?.kill('SIGKILL');
          resolve();
        }, 5000);

        this._process?.on('exit', () => {
          clearTimeout(timer);
          resolve();
        });
      });
    }

    this._client = null;
    this._process = null;

    if (this._exitHandler) {
      process.removeListener('exit', this._exitHandler);
      this._exitHandler = null;
    }
  }

  get isLaunched(): boolean {
    return this._client !== null;
  }

  locator(selector: string, opts?: LocatorOptions): JavaFxLocator {
    const client = this._ensureClient();
    return new JavaFxLocator(
      client,
      selector,
      opts?.timeout ?? this._config.defaultTimeoutMs,
      this._config.pollIntervalMs,
    );
  }

  async screenshot(name: string, opts?: ScreenshotOptions): Promise<Buffer> {
    const client = this._ensureClient();
    fs.mkdirSync(this._config.screenshotDir, { recursive: true });

    const response = await client.captureScreenshot({
      selector: opts?.selector,
      windowIndex: opts?.windowIndex,
    });
    const buffer = Buffer.from(response.data, 'base64');
    const filePath = path.join(this._config.screenshotDir, `${name}.png`);
    fs.writeFileSync(filePath, buffer);
    return buffer;
  }

  async takeScreenshot(name: string): Promise<Buffer> {
    return this.screenshot(name);
  }

  private _ensureClient(): JavaFxClient {
    if (!this._client) {
      throw new Error('Driver not launched. Call launch() or connect() first.');
    }
    return this._client;
  }

  private _cleanup(): void {
    if (this._process && !this._process.killed) {
      this._process.kill('SIGKILL');
    }
  }

  private detectPort(timeoutMs: number, stderr: string): Promise<number> {
    if (this._config.agentPort !== 0) {
      return Promise.resolve(this._config.agentPort);
    }

    return new Promise<number>((resolve, reject) => {
      const timer = setTimeout(() => {
        reject(new LaunchError(
          this._config.javaBin,
          null,
          `Agent did not report port within ${timeoutMs}ms. stderr: ${stderr}`,
        ));
      }, timeoutMs);

      const onData = (chunk: Buffer) => {
        const lines = chunk.toString().split('\n');
        for (const line of lines) {
          const match = line.match(/FXAGENT_READY port=(\d+)/);
          if (match) {
            clearTimeout(timer);
            this._process?.stdout?.removeListener('data', onData);
            resolve(parseInt(match[1], 10));
            return;
          }
        }
      };

      this._process?.stdout?.on('data', onData);

      this._process?.on('exit', (code) => {
        clearTimeout(timer);
        reject(new LaunchError(this._config.javaBin, code, stderr));
      });
    });
  }

  private async waitForReady(timeoutMs: number, stderr: string): Promise<void> {
    try {
      await waitUntil(
        async () => {
          const ready = await this._client!.isReady();
          return ready ? true : null;
        },
        {
          timeoutMs,
          pollIntervalMs: 500,
          selector: 'agent',
          condition: 'ready',
        },
      );
    } catch {
      throw new ConnectionError(this._config.agentHost, this._port, new Error(
        `Agent did not become ready within ${timeoutMs}ms. stderr: ${stderr}`,
      ));
    }
  }
}

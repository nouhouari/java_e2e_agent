export class JavaFxError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'JavaFxError';
  }
}

export class ConnectionError extends JavaFxError {
  public readonly host: string;
  public readonly port: number;
  public readonly cause?: Error;

  constructor(host: string, port: number, cause?: Error) {
    super(`Cannot connect to JavaFX agent at ${host}:${port}`);
    this.name = 'ConnectionError';
    this.host = host;
    this.port = port;
    this.cause = cause;
  }
}

export class TimeoutError extends JavaFxError {
  public readonly selector: string;
  public readonly condition: string;
  public readonly timeoutMs: number;

  constructor(selector: string, condition: string, timeoutMs: number) {
    super(`Timeout ${timeoutMs}ms exceeded waiting for '${selector}' to be ${condition}`);
    this.name = 'TimeoutError';
    this.selector = selector;
    this.condition = condition;
    this.timeoutMs = timeoutMs;
  }
}

export class ActionError extends JavaFxError {
  public readonly selector: string;
  public readonly action: string;
  public readonly reason: string;

  constructor(selector: string, action: string, reason: string) {
    super(`Cannot ${action} on '${selector}': ${reason}`);
    this.name = 'ActionError';
    this.selector = selector;
    this.action = action;
    this.reason = reason;
  }
}

export class ProtocolError extends JavaFxError {
  public readonly endpoint: string;
  public readonly statusCode: number;
  public readonly agentError?: string;

  constructor(endpoint: string, statusCode: number, agentError?: string) {
    super(`Agent returned error for ${endpoint} (HTTP ${statusCode})${agentError ? ': ' + agentError : ''}`);
    this.name = 'ProtocolError';
    this.endpoint = endpoint;
    this.statusCode = statusCode;
    this.agentError = agentError;
  }
}

export class LaunchError extends JavaFxError {
  public readonly command: string;
  public readonly exitCode: number | null;
  public readonly stderr?: string;

  constructor(command: string, exitCode: number | null, stderr?: string) {
    const details = stderr ? `: ${stderr}` : '';
    super(`Failed to launch JavaFX application (exit code ${exitCode})${details}`);
    this.name = 'LaunchError';
    this.command = command;
    this.exitCode = exitCode;
    this.stderr = stderr;
  }
}

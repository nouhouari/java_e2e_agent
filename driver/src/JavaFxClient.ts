import { ConnectionError, ProtocolError } from './errors';
import type {
  AgentNodeInfo,
  AgentQueryResponse,
  AgentActionResponse,
  AgentHealthResponse,
  AgentScreenshotResponse,
} from './types';

const DEFAULT_REQUEST_TIMEOUT_MS = 10_000;

export class JavaFxClient {
  private readonly baseUrl: string;
  private readonly requestTimeoutMs: number;

  constructor(host: string, port: number, requestTimeoutMs?: number) {
    this.baseUrl = `http://${host}:${port}`;
    this.requestTimeoutMs = requestTimeoutMs ?? DEFAULT_REQUEST_TIMEOUT_MS;
  }

  async isReady(): Promise<boolean> {
    try {
      await this.request<AgentHealthResponse>('GET', '/api/v1/health');
      return true;
    } catch {
      return false;
    }
  }

  async queryNode(selector: string, windowIndex?: number): Promise<AgentQueryResponse> {
    const body: Record<string, unknown> = { selector, maxResults: 1 };
    if (windowIndex !== undefined) body.windowIndex = windowIndex;
    return this.request<AgentQueryResponse>('POST', '/api/v1/elements/query', body);
  }

  async queryAll(selector: string, windowIndex?: number): Promise<AgentQueryResponse> {
    const body: Record<string, unknown> = { selector, maxResults: 1000 };
    if (windowIndex !== undefined) body.windowIndex = windowIndex;
    return this.request<AgentQueryResponse>('POST', '/api/v1/elements/query', body);
  }

  async performAction(
    selector: string,
    action: string,
    params?: Record<string, unknown>,
  ): Promise<AgentActionResponse> {
    const body: Record<string, unknown> = { selector, action };
    if (params) {
      if ('value' in params) body.value = params.value;
      const options: Record<string, string> = {};
      for (const [k, v] of Object.entries(params)) {
        if (k !== 'value') options[k] = String(v);
      }
      if (Object.keys(options).length > 0) body.options = options;
    }
    return this.request<AgentActionResponse>('POST', '/api/v1/actions', body);
  }

  async captureScreenshot(opts?: { selector?: string; windowIndex?: number }): Promise<AgentScreenshotResponse> {
    return this.request<AgentScreenshotResponse>('POST', '/api/v1/screenshot', opts ?? {});
  }

  async getElement(handle: string): Promise<AgentNodeInfo> {
    return this.request<AgentNodeInfo>('GET', `/api/v1/elements/${encodeURIComponent(handle)}`);
  }

  private async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), this.requestTimeoutMs);

    try {
      const headers: Record<string, string> = {};
      let requestBody: string | undefined;

      if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
        requestBody = JSON.stringify(body);
      }

      const response = await fetch(url, {
        method,
        headers,
        body: requestBody,
        signal: controller.signal,
      });

      const responseBody = await response.text();
      let parsed: unknown;
      try {
        parsed = JSON.parse(responseBody);
      } catch {
        parsed = responseBody;
      }

      if (!response.ok) {
        const agentError =
          typeof parsed === 'object' && parsed !== null && 'error' in parsed
            ? String((parsed as { error: unknown }).error)
            : typeof parsed === 'string' && parsed.length > 0
              ? parsed
              : undefined;
        throw new ProtocolError(path, response.status, agentError);
      }

      return parsed as T;
    } catch (err) {
      if (err instanceof ProtocolError) throw err;

      const error = err as NodeJS.ErrnoException;
      if (
        error.name === 'AbortError' ||
        error.code === 'ECONNREFUSED' ||
        error.code === 'ECONNRESET' ||
        error.code === 'UND_ERR_CONNECT_TIMEOUT' ||
        error.cause instanceof Error
      ) {
        const urlObj = new URL(this.baseUrl);
        throw new ConnectionError(urlObj.hostname, Number(urlObj.port), error);
      }

      throw error;
    } finally {
      clearTimeout(timer);
    }
  }
}

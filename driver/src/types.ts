// ---- Launch & Config ----

export interface JavaFxConfig {
  javaBin?: string;
  agentJar: string;
  agentPort?: number;
  agentHost?: string;
  defaultTimeoutMs?: number;
  pollIntervalMs?: number;
  screenshotDir?: string;
  jvmArgs?: string[];
}

export interface JavaFxLaunchOptions {
  app: string;
  agentJar?: string;
  classpath?: string | string[];
  modulePath?: string | string[];
  jvmArgs?: string[];
  env?: Record<string, string>;
  cwd?: string;
  readyTimeoutMs?: number;
}

export interface LocatorOptions {
  timeout?: number;
}

export interface FillOptions {
  clear?: boolean;
  timeout?: number;
}

export interface ClickOptions {
  clickCount?: number;
  timeout?: number;
  strategy?: 'default' | 'robot';
}

export interface SelectOptions {
  timeout?: number;
}

export interface ScreenshotOptions {
  selector?: string;
  windowIndex?: number;
}

export interface WaitForOptions {
  state?: 'visible' | 'hidden' | 'enabled' | 'disabled';
  timeout?: number;
}

// ---- Agent Protocol DTOs ----

export interface AgentNodeInfo {
  handle: string;
  id: string | null;
  type: string;
  fullType: string;
  styleClasses: string[];
  text: string | null;
  visible: boolean;
  enabled: boolean;
  disabled: boolean;
  focused: boolean;
  bounds: { x: number; y: number; width: number; height: number } | null;
  properties: Record<string, unknown> | null;
}

export interface AgentResponse<T = unknown> {
  success: boolean;
  data?: T;
  error?: string;
  errorType?: string;
}

export interface AgentQueryResponse {
  elements: AgentNodeInfo[];
  count: number;
}

export interface AgentActionResponse {
  success: boolean;
  message: string;
  element?: AgentNodeInfo;
}

export interface AgentHealthResponse {
  status: string;
  version: string;
}

export interface AgentReadyResponse {
  status: string;
  windowCount: number;
}

export interface AgentScreenshotResponse {
  data: string;
  format: string;
  width: number;
  height: number;
}

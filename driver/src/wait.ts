import { TimeoutError } from './errors';

export interface WaitOptions {
  timeoutMs: number;
  pollIntervalMs: number;
  selector: string;
  condition: string;
}

export async function waitUntil<T>(
  fn: () => Promise<T | null>,
  opts: WaitOptions,
): Promise<T> {
  const deadline = Date.now() + opts.timeoutMs;

  while (Date.now() < deadline) {
    const result = await fn();
    if (result !== null) {
      return result;
    }
    const remaining = deadline - Date.now();
    if (remaining <= 0) break;
    await sleep(Math.min(opts.pollIntervalMs, remaining));
  }

  // One final attempt after loop
  const finalResult = await fn();
  if (finalResult !== null) {
    return finalResult;
  }

  throw new TimeoutError(opts.selector, opts.condition, opts.timeoutMs);
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, Math.max(0, ms)));
}

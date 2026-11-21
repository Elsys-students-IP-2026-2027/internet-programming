import { RetriesExhaustedError, ServerError, ServerUnavailableError } from './errors.js';
import { log } from './log.js';

// Promise-based pause - the counterpart of CompletableFuture.delayedExecutor (no blocking sleep).
const delay = (seconds) => new Promise((resolve) => setTimeout(resolve, seconds * 1000));

const isRetryable = (error) => error instanceof ServerUnavailableError || error instanceof ServerError;

// Repeats only on ServerUnavailable (connection refused / timeout) and 5xx, never on 4xx.
// Pauses grow 1, 2, 4, 8 s.
export function withRetry(call, maxAttempts, attempt = 1) {
  return Promise.resolve()
    .then(call)
    .catch((error) => {
      if (!isRetryable(error)) {
        throw error;
      }
      if (attempt >= maxAttempts) {
        log.error(`Опит ${attempt}/${maxAttempts} неуспешен: ${error.reason}, X-Request-Id: ${error.requestId}. Спираме.`);
        throw new RetriesExhaustedError(`${error.reason} след ${maxAttempts} опита`, error);
      }

      const seconds = 2 ** (attempt - 1);
      log.warn(`Опит ${attempt}/${maxAttempts} неуспешен: ${error.reason}, X-Request-Id: ${error.requestId}. `
        + `Нов опит след ${seconds} s`);
      return delay(seconds).then(() => withRetry(call, maxAttempts, attempt + 1));
    });
}

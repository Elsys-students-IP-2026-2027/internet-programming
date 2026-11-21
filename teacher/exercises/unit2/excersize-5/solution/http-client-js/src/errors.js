// Base class for every failed HTTP call. Keeps the X-Request-Id so it can be logged on retry.
export class HttpCallError extends Error {
  constructor(message, requestId, cause) {
    super(message, { cause });
    this.name = this.constructor.name;
    this.requestId = requestId;
  }

  get reason() {
    return this.message;
  }
}

// The server answered, but with an error status code.
export class HttpStatusError extends HttpCallError {
  constructor(status, body, requestId) {
    super(`HTTP ${status}: ${body}`, requestId);
    this.status = status;
    this.body = body;
  }

  get reason() {
    return `HTTP ${this.status}`;
  }
}

// 4xx - the request is wrong, repeating it will not help.
export class ClientError extends HttpStatusError {}

// 5xx - the server failed, the same request may succeed later.
export class ServerError extends HttpStatusError {}

// No HTTP response at all: connection refused (server stopped) or timeout.
export class ServerUnavailableError extends HttpCallError {
  constructor(requestId, cause) {
    const isTimeout = cause?.name === 'TimeoutError' || cause?.name === 'AbortError';
    const code = cause?.cause?.code ?? cause?.code ?? cause?.name;
    super(isTimeout ? 'timeout' : `сървърът не отговаря (${code})`, requestId, cause);
  }
}

export class RetriesExhaustedError extends Error {
  constructor(message, cause) {
    super(message, { cause });
    this.name = 'RetriesExhaustedError';
  }
}

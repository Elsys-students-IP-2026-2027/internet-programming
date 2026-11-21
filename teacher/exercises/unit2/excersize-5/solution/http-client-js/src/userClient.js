import { randomUUID } from 'node:crypto';
import { ClientError, ServerError, ServerUnavailableError } from './errors.js';
import { log } from './log.js';

const JSON_TYPE = 'application/json';
const REQUEST_ID = 'X-Request-Id';
const TIMEOUT_MS = 5000;

export const newRequestId = () => randomUUID();

// In JavaScript every HTTP call is asynchronous (fetch returns a Promise).
// The Java "sync" methods correspond to awaiting each call one after another (see syncDemo.js);
// the Java "async" methods correspond to chaining the Promises (see asyncDemo.js).
export class UserClient {
  constructor(baseUrl) {
    this.baseUrl = baseUrl.replace(/\/$/, '');
  }

  async create(user, requestId = newRequestId()) {
    return toUser(await this.#send('POST', '/users', user, requestId));
  }

  // 404 -> null (the JavaScript counterpart of Optional.empty()).
  async getById(id) {
    return toOptionalUser(await this.#send('GET', `/users/${id}`));
  }

  async getAll() {
    const response = await this.#send('GET', '/users');
    ensureSuccess(response);
    return response.data;
  }

  // 404 -> ClientError: there is no user to return.
  async patch(id, fields) {
    return toUser(await this.#send('PATCH', `/users/${id}`, fields));
  }

  async delete(id) {
    const response = await this.#send('DELETE', `/users/${id}`);
    if (response.status === 404) {
      return false;
    }
    ensureSuccess(response);
    return true;
  }

  async #send(method, path, body, requestId = newRequestId()) {
    const headers = { Accept: JSON_TYPE, [REQUEST_ID]: requestId };
    if (body !== undefined) {
      headers['Content-Type'] = JSON_TYPE;
    }

    const start = performance.now();
    let response;
    let text;
    try {
      response = await fetch(this.baseUrl + path, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: AbortSignal.timeout(TIMEOUT_MS),
      });
      text = await response.text();
    } catch (error) {
      log.warn(`${method} ${path}, Failed: ${error.cause?.code ?? error.name}, `
        + `Duration: ${ms(start)} ms, ${REQUEST_ID}: ${requestId}`);
      throw new ServerUnavailableError(requestId, error);
    }

    log.info(`${method} ${path}, Status: ${response.status}, Duration: ${ms(start)} ms, ${REQUEST_ID}: ${requestId}`);
    return { status: response.status, text, data: parse(text), requestId };
  }
}

function toUser(response) {
  ensureSuccess(response);
  return response.data;
}

function toOptionalUser(response) {
  return response.status === 404 ? null : toUser(response);
}

// 2xx -> ok, 4xx -> ClientError, 5xx -> ServerError.
function ensureSuccess({ status, text, requestId }) {
  if (status >= 200 && status < 300) {
    return;
  }
  throw status >= 500
    ? new ServerError(status, text, requestId)
    : new ClientError(status, text, requestId);
}

function parse(text) {
  try {
    return text ? JSON.parse(text) : null;
  } catch {
    return null;
  }
}

const ms = (start) => Math.round(performance.now() - start);

package org.example.client.exception;

/// Base class for every failed HTTP call. Keeps the X-Request-Id so it can be logged on retry.
public abstract class HttpCallException extends RuntimeException {
    private final String requestId;

    protected HttpCallException(String message, String requestId, Throwable cause) {
        super(message, cause);
        this.requestId = requestId;
    }

    public String getRequestId() {
        return requestId;
    }

    /// Short reason for logs, e.g. "HTTP 503" or "сървърът не отговаря".
    public abstract String reason();
}

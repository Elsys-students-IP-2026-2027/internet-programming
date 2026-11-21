package org.example.client.exception;

import java.net.http.HttpTimeoutException;

/// No HTTP response at all: connection refused (server stopped) or timeout.
public class ServerUnavailableException extends HttpCallException {
    public ServerUnavailableException(String requestId, Throwable cause) {
        super(describe(cause), requestId, cause);
    }

    private static String describe(Throwable cause) {
        return cause instanceof HttpTimeoutException
                ? "timeout"
                : "сървърът не отговаря (" + cause.getClass().getSimpleName() + ")";
    }

    @Override
    public String reason() {
        return getMessage();
    }
}

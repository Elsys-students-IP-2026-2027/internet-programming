package org.example.client.exception;

/// 5xx - the server failed, the same request may succeed later.
public class ServerErrorException extends HttpStatusException {
    public ServerErrorException(int status, String body, String requestId) {
        super(status, body, requestId);
    }
}

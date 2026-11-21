package org.example.client.exception;

/// The server answered, but with an error status code.
public abstract class HttpStatusException extends HttpCallException {
    private final int status;
    private final String body;

    protected HttpStatusException(int status, String body, String requestId) {
        super("HTTP " + status + ": " + body, requestId, null);
        this.status = status;
        this.body = body;
    }

    public int getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String reason() {
        return "HTTP " + status;
    }
}

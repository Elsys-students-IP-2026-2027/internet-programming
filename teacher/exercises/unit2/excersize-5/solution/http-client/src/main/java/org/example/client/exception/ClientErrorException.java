package org.example.client.exception;

/// 4xx - the request is wrong, repeating it will not help.
public class ClientErrorException extends HttpStatusException {
    public ClientErrorException(int status, String body, String requestId) {
        super(status, body, requestId);
    }
}

package org.example.client.exception;

public class RetriesExhaustedException extends RuntimeException {
    public RetriesExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }
}

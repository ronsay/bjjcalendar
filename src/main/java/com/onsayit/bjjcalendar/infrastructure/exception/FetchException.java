package com.onsayit.bjjcalendar.infrastructure.exception;

public class FetchException extends RuntimeException {
    public FetchException(final String message) {
        super(message);
    }

    public FetchException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

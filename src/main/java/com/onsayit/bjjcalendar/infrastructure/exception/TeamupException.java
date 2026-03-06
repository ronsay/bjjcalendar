package com.onsayit.bjjcalendar.infrastructure.exception;

import com.onsayit.bjjcalendar.infrastructure.teamup.ApiException;

public class TeamupException extends RuntimeException {

    public TeamupException(final String message, final ApiException e) {
        super(message, e);
    }
}

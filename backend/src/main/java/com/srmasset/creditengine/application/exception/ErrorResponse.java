package com.srmasset.creditengine.application.exception;

import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;

public record ErrorResponse(OffsetDateTime timestamp, int status, String error, String message) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(
                OffsetDateTime.now(), status.value(), status.getReasonPhrase(), message);
    }
}

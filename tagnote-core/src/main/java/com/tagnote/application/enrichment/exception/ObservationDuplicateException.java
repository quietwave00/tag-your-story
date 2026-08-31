package com.tagnote.application.enrichment.exception;

public class ObservationDuplicateException extends RuntimeException {

    public ObservationDuplicateException(Throwable cause) {
        super("External tag observation already exists", cause);
    }
}

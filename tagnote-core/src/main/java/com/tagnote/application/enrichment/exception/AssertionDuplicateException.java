package com.tagnote.application.enrichment.exception;

public class AssertionDuplicateException extends RuntimeException {

    public AssertionDuplicateException(Throwable cause) {
        super("Tag assertion already exists", cause);
    }
}

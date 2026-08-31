package com.tagnote.application.resolution.exception;

public class ResolvedTagDuplicateException extends RuntimeException {

    public ResolvedTagDuplicateException(Throwable cause) {
        super("Resolved tag already exists", cause);
    }
}

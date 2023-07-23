package com.tagstory.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final ExceptionCode exceptionCode;
    private final String message;

    public CustomException(ExceptionCode exceptionCode) {
        this.exceptionCode = exceptionCode;
        this.message = exceptionCode.getMessage();
    }
}

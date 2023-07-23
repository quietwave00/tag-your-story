package com.tagstory.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExceptionResponse {
    private ExceptionCode exceptionCode;
    private String message;
    private int status;
}

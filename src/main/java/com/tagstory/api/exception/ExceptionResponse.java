package com.tagstory.api.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ExceptionResponse {
    private ExceptionCode exceptionCode;
    private String message;
    private int status;
}

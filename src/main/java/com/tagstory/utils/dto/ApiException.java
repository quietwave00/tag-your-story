package com.tagstory.utils.dto;

import com.tagstory.exception.ExceptionCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiException extends RuntimeException {
    private ExceptionCode exceptionCode;
}

package com.tagstory.api.exception;

import com.tagstory.api.utils.dto.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ExceptionManager {
    @ExceptionHandler(CustomException.class)
    public ApiResult<ExceptionResponse> handleApiException(CustomException e) {
        log.error("exceptionCode: {}, message: {}", e.getExceptionCode(), e.getMessage());
        return ApiResult.result(false, ExceptionResponse.builder()
                .exceptionCode(e.getExceptionCode())
                .message(e.getMessage())
                .status(e.getExceptionCode().getHttpStatus().value())
                .build());
    }
}

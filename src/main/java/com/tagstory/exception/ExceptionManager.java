package com.tagstory.exception;

import com.tagstory.utils.dto.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ExceptionManager {
    @ExceptionHandler(CustomException.class)
    public ApiResult<ExceptionResponse> handleApiException(CustomException e) {
        log.error("exceptionCode: {}, message: {}", e.getExceptionCode(), e.getMessage());
        return ApiResult.result(false, new ExceptionResponse(e.getExceptionCode(), e.getMessage(), e.getExceptionCode().getHttpStatus().value()));
    }
}

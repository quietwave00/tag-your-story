package com.tagstory.core.exception;

import com.tagstory.core.utils.api.dto.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ValidExceptionManager {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<ExceptionResponse> handleApiException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();
        log.error("message: {}", errorMessage);

        return ApiResult.result(false, ExceptionResponse.builder()
                .message(e.getMessage())
                .build());
    }
}

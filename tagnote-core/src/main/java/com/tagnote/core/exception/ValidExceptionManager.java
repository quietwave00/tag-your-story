package com.tagnote.core.exception;

import com.tagnote.core.utils.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ValidExceptionManager {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<ExceptionResponse> handleApiException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();
        log.error("message: {}", errorMessage);

        return badRequest(errorMessage);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<ExceptionResponse> handleUnreadableRequest(HttpMessageNotReadableException e) {
        log.error("message: {}", e.getMessage());
        return badRequest("요청 본문을 읽을 수 없습니다.");
    }

    private ApiResult<ExceptionResponse> badRequest(String message) {
        return ApiResult.result(false, ExceptionResponse.builder()
                .message(message)
                .status(HttpStatus.BAD_REQUEST.value())
                .build());
    }
}

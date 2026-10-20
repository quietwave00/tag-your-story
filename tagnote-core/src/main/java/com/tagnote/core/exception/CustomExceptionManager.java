package com.tagnote.core.exception;

import com.tagnote.core.utils.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class CustomExceptionManager {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResult<ExceptionResponse>> handleApiException(CustomException e) {
        log.error("exceptionCode: {}, message: {}", e.getExceptionCode(), e.getMessage());
        ApiResult<ExceptionResponse> result = ApiResult.result(false, ExceptionResponse.builder()
                .exceptionCode(e.getExceptionCode())
                .message(e.getMessage())
                .status(e.getExceptionCode().getHttpStatus().value())
                .build());
        if (e.getExceptionCode() == ExceptionCode.TOKEN_HAS_EXPIRED
                || e.getExceptionCode() == ExceptionCode.TOKEN_HAS_TEMPERED
                || e.getExceptionCode() == ExceptionCode.INVALID_REFRESH_TOKEN) {
            return ResponseEntity.status(e.getExceptionCode().getHttpStatus()).body(result);
        }
        return ResponseEntity.ok(result);
    }
}

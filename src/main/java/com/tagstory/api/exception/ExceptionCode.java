package com.tagstory.api.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExceptionCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저가 존재하지 않습니다."),
    TOKEN_HAS_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    TOKEN_HAS_TEMPERED(HttpStatus.UNAUTHORIZED, "위변조된 토큰입니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않는 토큰입니다"),
    CONVERSION_EXCEPTION(HttpStatus.UNPROCESSABLE_ENTITY, "데이터 변환 중 예외가 발생했습니다."),
    S3_UPLOAD_EXCEPTION(HttpStatus.SERVICE_UNAVAILABLE, "파일 업로드 중 예외가 발생했습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;
}

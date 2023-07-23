package com.tagstory.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExceptionCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "유저가 존재하지 않습니다."),
    TOKEN_HAS_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    TOKEN_HAS_TEMPERED(HttpStatus.UNAUTHORIZED, "위변조된 토큰입니다")
    ;

    private final HttpStatus httpStatus;
    private final String message;
}

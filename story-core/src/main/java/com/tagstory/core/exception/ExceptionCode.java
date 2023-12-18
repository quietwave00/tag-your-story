package com.tagstory.core.exception;

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
    S3_UPLOAD_EXCEPTION(HttpStatus.SERVICE_UNAVAILABLE, "파일 업로드 중 예외가 발생했습니다."),
    SPOTIFY_EXCEPTION(HttpStatus.SERVICE_UNAVAILABLE, "스포티파이 라이브러리 사용 중 예외가 발생했습니다."),
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시물입니다."),
    HASHTAG_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 해시태그입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),
    NO_READ_PERMISSION(HttpStatus.FORBIDDEN, "조회 권한이 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;
}

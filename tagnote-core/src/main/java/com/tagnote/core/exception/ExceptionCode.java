package com.tagnote.core.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ExceptionCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저입니다."),
    GOOGLE_EMAIL_UNVERIFIED(HttpStatus.UNAUTHORIZED, "Google 이메일 인증이 필요합니다."),
    GOOGLE_IDENTITY_INVALID(HttpStatus.UNAUTHORIZED, "Google 계정 식별자가 유효하지 않습니다."),
    GOOGLE_EMAIL_CONFLICT_LOCAL(HttpStatus.CONFLICT, "이미 이메일로 가입한 계정입니다."),
    GOOGLE_EMAIL_CONFLICT_GOOGLE(HttpStatus.CONFLICT, "다른 Google 계정에 등록된 이메일입니다."),
    GOOGLE_SIGNUP_CONFLICT(HttpStatus.CONFLICT, "Google 가입이 충돌했습니다. 다시 시도해 주세요."),
    SIGNUP_CHALLENGE_INVALID(HttpStatus.BAD_REQUEST, "가입 인증이 만료되었거나 유효하지 않습니다."),
    SIGNUP_CODE_INVALID(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않습니다."),
    SIGNUP_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "가입 인증 요청이 너무 많습니다."),
    SIGNUP_MAIL_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "인증 메일을 보낼 수 없습니다."),
    SIGNUP_CHALLENGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "가입 인증 저장소를 사용할 수 없습니다."),
    GOOGLE_ACCOUNT_EXISTS(HttpStatus.CONFLICT, "Google 계정으로 가입된 이메일입니다."),
    LOCAL_ACCOUNT_EXISTS(HttpStatus.CONFLICT, "이미 이메일로 가입한 계정입니다."),
    SIGNUP_INPUT_INVALID(HttpStatus.BAD_REQUEST, "가입 입력이 유효하지 않습니다."),
    SIGNUP_CONFIRM_BUSY(HttpStatus.CONFLICT, "가입 확인이 처리 중입니다. 다시 시도해 주세요."),
    SIGNUP_CONFLICT(HttpStatus.CONFLICT, "가입이 충돌했습니다. 다시 시도해 주세요."),
    LOGIN_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    LOGIN_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해 주세요."),
    LOGIN_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "로그인 제한 저장소를 사용할 수 없습니다."),
    USER_INACTIVE(HttpStatus.FORBIDDEN, "비활성 계정입니다."),
    ACCOUNT_WITHDRAWN(HttpStatus.UNAUTHORIZED, "탈퇴한 계정입니다."),
    NICKNAME_INVALID(HttpStatus.BAD_REQUEST, "닉네임은 공백 없이 1~20자여야 합니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시물입니다."),
    LIKE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 좋아요한 게시물입니다."),
    TRACK_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 트랙입니다."),
    ALBUM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 앨범입니다."),
    USER_TAG_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 유저 태그입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다."),

    TOKEN_HAS_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),
    TOKEN_HAS_TEMPERED(HttpStatus.UNAUTHORIZED, "위변조된 토큰입니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않는 토큰입니다"),
    GOOGLE_RELOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "Google 재로그인이 필요합니다."),

    CONVERSION_EXCEPTION(HttpStatus.UNPROCESSABLE_ENTITY, "데이터 변환 중 예외가 발생했습니다."),

    FILE_STORAGE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "파일 스토리지가 설정되지 않았습니다."),
    SPOTIFY_EXCEPTION(HttpStatus.SERVICE_UNAVAILABLE, "스포티파이 라이브러리 사용 중 예외가 발생했습니다."),

    LOCKED_RESOURCE(HttpStatus.LOCKED, "사용할 수 없는 자원입니다."),

    NO_READ_PERMISSION(HttpStatus.FORBIDDEN, "조회 권한이 없습니다."),
    NO_PERMISSION(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    BOARD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "게시물 작성자만 변경할 수 있습니다."),
    INVALID_BOARD_SUBJECT(HttpStatus.BAD_REQUEST, "trackId와 albumId 중 하나만 입력해야 합니다.")
    ;

    private final HttpStatus httpStatus;
    private final String message;
}

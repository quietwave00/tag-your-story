package com.tagstory.api.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.core.exception.CustomException;
import com.tagstory.core.exception.ExceptionCode;
import com.tagstory.core.exception.ExceptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomExceptionDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /**
     *  SpringSecurity가 처리하는 인증 인가 에러에 대해 커스텀하여 클라이언트에 메시지를 보여준다.
     */
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) {
        CustomException exception = new CustomException(ExceptionCode.NO_PERMISSION);
        ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                .exceptionCode(exception.getExceptionCode())
                .message(exception.getMessage())
                .status(exception.getExceptionCode().getHttpStatus().value())
                .build();

        try {
            String exceptionMessage = objectMapper.writeValueAsString(exceptionResponse);
            response.getOutputStream().write(exceptionMessage.getBytes());
            response.getOutputStream().flush();
        } catch(IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}

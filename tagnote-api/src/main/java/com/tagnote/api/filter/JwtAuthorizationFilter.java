package com.tagnote.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.api.auth.PrincipalDetails;
import com.tagnote.core.domain.user.service.User;
import com.tagnote.core.domain.user.service.UserService;
import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionResponse;
import com.tagnote.core.utils.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

import static com.tagnote.core.utils.jwt.JwtProperties.*;


@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final UserService userService;

    private static final String EMPTY_TOKEN = "null";

    /*
     * 인증 정보를 추출하고, 유효한 사용자인지 검증한다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if ("POST".equals(request.getMethod()) && ("/api/user/reissue/accessToken".equals(request.getServletPath())
                || "/api/auth/login".equals(request.getServletPath()))) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = extractToken(request);

        try {
            /* No token: let Spring Security's anonymous filter handle the request. */
            if (!StringUtils.hasText(token) || EMPTY_TOKEN.equals(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            if (token.startsWith(TOKEN_PREFIX)) {
                token = token.substring(TOKEN_PREFIX.length());
            }

            handleUserRequest(request, response, filterChain, token);
        } catch(CustomException e) {
            sendExceptionMessage(response, e);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String headerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (Objects.nonNull(headerToken)) {
            return headerToken;
        }

        String queryToken = request.getParameter(HttpHeaders.AUTHORIZATION);
        if (Objects.nonNull(queryToken)) {
            return queryToken;
        }
        return null;
    }

    private void handleUserRequest(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain,
                                   String token) throws IOException, ServletException {
        try {
            Long userId = jwtUtil.getUserIdFromToken(token, TOKEN_TYPE_ACCESS);
            setAuthentication(userId);
            filterChain.doFilter(request, response);
        } catch (CustomException e) {
            sendExceptionMessage(response, e);
        }
    }

    private void setAuthentication(Long userId) {
        User user = userService.requireActiveUser(userId);
        PrincipalDetails principalDetails = new PrincipalDetails(user.getUserId(), user.getRole(), user.getNickname());
        Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /*
     * 클라이언트에 예외 메시지를 전달해준다.
     */
    private void sendExceptionMessage(HttpServletResponse response, CustomException exception) {
        try {
            response.setStatus(exception.getExceptionCode().getHttpStatus().value());
            response.setContentType("application/json");
            ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                            .exceptionCode(exception.getExceptionCode())
                            .message(exception.getMessage())
                            .status(exception.getExceptionCode().getHttpStatus().value())
                            .build();
            objectMapper.writeValue(response.getOutputStream(), exceptionResponse);
        } catch(IOException e) {
            log.error(e.getMessage());
        }
    }

}

package com.tagstory.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.api.auth.PrincipalDetails;
import com.tagstory.core.domain.user.Role;
import com.tagstory.core.exception.CustomException;
import com.tagstory.core.exception.ExceptionResponse;
import com.tagstory.core.utils.jwt.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import static com.tagstory.core.utils.jwt.JwtProperties.*;


@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    private static final String EMPTY_TOKEN = "null";

    /*
     * 인증 정보를 추출하고, 유효한 사용자인지 검증한다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        try {
            /* 토큰이 없으면 게스트 권한을 부여한다. */
            if (!StringUtils.hasText(token) || EMPTY_TOKEN.equals(token)) {
                handleGuestRequest(request, response, filterChain);
                return;
            }

            /* 토큰 타입이 PENDING이면 PENDING 권한을 부여한다. */
            if (TOKEN_TYPE_PENDING.equals(jwtUtil.getTokenTypeFromToken(token))) {
                handlePendingUserRequest(request, response, filterChain, token);
                return;
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

    private void handleGuestRequest(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws IOException, ServletException {
        registerAsGuest();
        filterChain.doFilter(request, response);
    }

    private void handlePendingUserRequest(HttpServletRequest request,
                                          HttpServletResponse response,
                                          FilterChain filterChain,
                                          String token) throws IOException, ServletException {
        registerAsPendingUser(jwtUtil.getPendingIdFromToken(token));
        filterChain.doFilter(request, response);
    }

    private void handleUserRequest(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain,
                                   String token) throws IOException, ServletException {
        try {
            jwtUtil.validateToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token.replace(TOKEN_PREFIX, ""));
            setAuthentication(userId);
            filterChain.doFilter(request, response);
        } catch (CustomException e) {
            sendExceptionMessage(response, e);
        }
    }

    private void setAuthentication(Long userId) {
        PrincipalDetails principalDetails = new PrincipalDetails(userId, Role.ROLE_USER);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /*
     * Guest 권한을 부여해준다.
     */
    private void registerAsGuest() {
        PrincipalDetails principalDetails = new PrincipalDetails(null, Role.ROLE_GUEST);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /*
     * Pending User 권한을 부여해준다.
     */
    private void registerAsPendingUser(String pendingId) {
        PrincipalDetails principalDetails = new PrincipalDetails(pendingId);
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.ROLE_PENDING_USER.toString()));
        Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /*
     * 클라이언트에 예외 메시지를 전달해준다.
     */
    private void sendExceptionMessage(HttpServletResponse response, CustomException exception) {
        try {
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

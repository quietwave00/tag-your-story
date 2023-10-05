package com.tagstory.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysql.cj.util.StringUtils;
import com.tagstory.api.auth.PrincipalDetails;
import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionResponse;
import com.tagstory.api.jwt.JwtUtil;
import com.tagstory.core.domain.user.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import static com.tagstory.api.jwt.JwtProperties.*;


@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    /*
     * 인증 정보를 추출하고, 유효한 사용자인지 검증한다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("JwtAuthorizationFilter Execute");
        String token = request.getHeader(HEADER_STRING);

        /* 토큰이 존재하지 않으면 게스트 역할이 부여된다. */
        if(StringUtils.isNullOrEmpty(token)) {
            log.info("Request As Guest");
            registerAsGuest();
            filterChain.doFilter(request, response);
            return;
        }

        /* 회원 가입이 완료되지 않은 역할이 부여된다. */
        if(TOKEN_TYPE_TEMP.equals(jwtUtil.getTokenTypeFromToken(token))) {
            log.info("Request As Pending User");
            registerAsPendingUser(jwtUtil.getTempIdFromToken(token));
            filterChain.doFilter(request, response);
            return;
        }

        /* 유저 권한을 갖는다 */
        try {
            jwtUtil.validateToken(token);
            Long userId = jwtUtil.getUserIdFromToken(request.getHeader(HEADER_STRING).replace(TOKEN_PREFIX, ""));
            log.info("Filter userId: {}", userId);

            PrincipalDetails principalDetails = new PrincipalDetails(userId, Role.ROLE_USER);
            Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("Authorization Complete");
            filterChain.doFilter(request, response);
        } catch(CustomException e) {
            sendExceptionMessage(response, e);
        }
    }

    /*
     * Guest 권한을 부여해준다.
     */
    private void registerAsGuest() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(Role.ROLE_GUEST.toString()));
        Authentication authentication = new UsernamePasswordAuthenticationToken(null, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /*
     * Pending User 권한을 부여해준다.
     */
    private void registerAsPendingUser(String tempId) {
        PrincipalDetails principalDetails = new PrincipalDetails(tempId);
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
            e.printStackTrace();
        }
    }
}

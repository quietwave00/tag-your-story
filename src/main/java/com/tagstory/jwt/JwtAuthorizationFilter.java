package com.tagstory.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.auth.PrincipalDetails;
import com.tagstory.entity.User;
import com.tagstory.exception.CustomException;
import com.tagstory.exception.ExceptionCode;
import com.tagstory.exception.ExceptionResponse;
import com.tagstory.user.cache.CacheUserRepository;
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


@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final CacheUserRepository cacheUserRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    /*
     * 인증 정보를 추출하고, 유효한 사용자인지 검증한다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("JwtAuthorizationFilter Execute");
        String token = request.getHeader("Authorization");

        if(token.isEmpty()) {
            registerAsGuest();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtUtil.validateToken(token);
            Long userId = jwtUtil.getUserIdFromJwt(request.getHeader("Authorization").replace("Bearer ", ""));
            User findUser = findUserByIdOrUserKey(userId, token);
            log.info("레디스에서 가져온 findUser: {}", findUser.toString());

            PrincipalDetails principalDetails = new PrincipalDetails(findUser);
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
    public void registerAsGuest() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_GUEST"));
        Authentication authentication = new UsernamePasswordAuthenticationToken(null, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /*
     * 클라이언트에 예외 메시지를 전달해준다.
     */
    public void sendExceptionMessage(HttpServletResponse response, CustomException exception) {
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


    private User findUserByIdOrUserKey(Long userId, String token) {
        return userId == null ? getUserFromRefreshToken(token) : getUserFromAuthorization(userId);
    }

    private User getUserFromAuthorization(final Long userId) {
        return cacheUserRepository.findUserByUserId(userId).orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
    }

    private User getUserFromRefreshToken(final String token) {
        Long userId = jwtUtil.getUserIdFromRefreshToken(token);
        return cacheUserRepository.findUserByUserId(userId).orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
    }
}

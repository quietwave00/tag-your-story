package com.tagstory.api.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionResponse;
import com.tagstory.api.auth.PrincipalDetails;
import com.tagstory.core.domain.user.User;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.repository.CacheUserRepository;
import com.tagstory.core.domain.user.redis.TagStoryRedisTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
    private final TagStoryRedisTemplate redisTemplate;
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
            Long userId = jwtUtil.getUserIdFromToken(request.getHeader("Authorization").replace("Bearer ", ""));
            User findUser = findUserById(userId, token);

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

    private User findUserById(Long userId, String token) {
        return userId == null ? getUserFromRefreshToken(token) : getUserFromAccessToken(userId);
    }

    @Cacheable(value = "user", key = "#userId")
    public User getUserFromAccessToken(final Long userId) {
        return cacheUserRepository.findByUserId(userId, CacheSpec.USER);
    }

    public User getUserFromRefreshToken(final String token) {
        Long userId = jwtUtil.getUserIdFromRefreshToken(token);
        return redisTemplate.get(userId, CacheSpec.REFRESH_TOKEN);
    }
}

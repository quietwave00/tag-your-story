package com.tagstory.jwt;

import com.tagstory.auth.PrincipalDetails;
import com.tagstory.entity.User;
import com.tagstory.exception.ExceptionCode;
import com.tagstory.user.repository.UserRepository;
import com.tagstory.utils.dto.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/*
 * 인증 정보를 추출하고, 유효한 사용자인지 검증한다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("JwtAuthorizationFilter Execute");
        String jwt = request.getHeader("Authorization");
        jwtUtil.validateJwt(jwt);

        String userKey = jwtUtil.getUserKeyFromJwt(request);
        log.info("userKey: {}", userKey);
        User findUser = userRepository.findByUserKey(userKey).orElseThrow(() -> new ApiException(ExceptionCode.USER_NOT_FOUND));

        PrincipalDetails principalDetails = new PrincipalDetails(findUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("Authorization Complete");
        filterChain.doFilter(request, response);
    }
}

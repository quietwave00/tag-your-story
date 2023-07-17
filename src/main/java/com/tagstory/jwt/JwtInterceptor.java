package com.tagstory.jwt;

import com.tagstory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final JwtCookieProvider jwtCookieProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("JwtInterceptor Execute");

        String jwt = request.getHeader("Authorization");
        if(jwtUtil.isTokenExpired(jwt)) {
            String userKey = jwtUtil.getUserKeyFromRefreshToken(request);
            String newJwt = jwtUtil.generateToken(userRepository.findByUserKey(userKey));
            response.addCookie(jwtCookieProvider.generateCookie(newJwt));
        }
        return true;
    }
}

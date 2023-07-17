package com.tagstory.config;

import com.tagstory.jwt.JwtCookieProvider;
import com.tagstory.jwt.JwtInterceptor;
import com.tagstory.jwt.JwtUtil;
import com.tagstory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final JwtCookieProvider jwtCookieProvider;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtInterceptor(jwtUtil, userRepository, jwtCookieProvider))
                .addPathPatterns("/api/user/**"); // JWT 검증이 필요한 API 경로를 지정
    }
}

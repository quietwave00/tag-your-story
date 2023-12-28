package com.tagstory.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.api.filter.JwtAuthorizationFilter;
import com.tagstory.api.oauth.OauthSuccessHandler;
import com.tagstory.api.oauth.PrincipalOauth2UserService;
import com.tagstory.core.utils.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfig corsConfig;
    private final PrincipalOauth2UserService principalOauth2UserService;
    private final OauthSuccessHandler oauthSuccessHandler;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable);

        http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        http
            .addFilter(corsConfig.corsFilter())
            .addFilterAfter(new JwtAuthorizationFilter(jwtUtil, objectMapper), UsernamePasswordAuthenticationFilter.class);

        http
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                            .userService(principalOauth2UserService))
                    .successHandler(oauthSuccessHandler));
        return http.build();
    }
}


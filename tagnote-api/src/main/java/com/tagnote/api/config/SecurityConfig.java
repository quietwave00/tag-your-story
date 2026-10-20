package com.tagnote.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.api.auth.CustomExceptionDeniedHandler;
import com.tagnote.api.auth.AnonymousPrincipal;
import com.tagnote.api.filter.JwtAuthorizationFilter;
import com.tagnote.api.oauth.OauthSuccessHandler;
import com.tagnote.api.oauth.OauthFailureHandler;
import com.tagnote.api.oauth.PrincipalOauth2UserService;
import com.tagnote.core.utils.jwt.JwtUtil;
import com.tagnote.core.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CorsConfig corsConfig;
    private final PrincipalOauth2UserService principalOauth2UserService;
    private final OauthSuccessHandler oauthSuccessHandler;
    private final OauthFailureHandler oauthFailureHandler;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final CustomExceptionDeniedHandler customExceptionDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling((exceptionConfig) ->
                    exceptionConfig
                    .accessDeniedHandler(customExceptionDeniedHandler)
                    .authenticationEntryPoint((request, response, exception) ->
                            customExceptionDeniedHandler.handle(request, response,
                                    new AccessDeniedException(exception.getMessage()))));

        http
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
            .anonymous(anonymous -> anonymous.principal(new AnonymousPrincipal()))
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable);

        http
            .addFilter(corsConfig.corsFilter())
            .addFilterAfter(new JwtAuthorizationFilter(jwtUtil, objectMapper, userService), UsernamePasswordAuthenticationFilter.class);

        http
            .oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                            .userService(principalOauth2UserService))
                    .successHandler(oauthSuccessHandler)
                    .failureHandler(oauthFailureHandler));
        return http.build();
    }
}


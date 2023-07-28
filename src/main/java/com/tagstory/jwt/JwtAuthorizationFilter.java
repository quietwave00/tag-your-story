package com.tagstory.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagstory.auth.PrincipalDetails;
import com.tagstory.entity.User;
import com.tagstory.exception.CustomException;
import com.tagstory.exception.ExceptionCode;
import com.tagstory.exception.ExceptionResponse;
import com.tagstory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Optional;


@Slf4j
@RequiredArgsConstructor
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /*
     * 인증 정보를 추출하고, 유효한 사용자인지 검증한다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("JwtAuthorizationFilter Execute");
        Optional<String> jwtOptional = Optional.ofNullable(request.getHeader("Authorization"));

        if(jwtOptional.isEmpty()) {
            registerAsGuest();
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = jwtOptional.get();
        if(StringUtils.hasText(jwt)) {
            try {
                jwtUtil.validateToken(jwt);
                Long userId = jwtUtil.getUserIdFromJwt(request);
                User findUser = userRepository.findByUserId(userId).orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
                PrincipalDetails principalDetails = new PrincipalDetails(findUser);
                Authentication authentication = new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Authorization Complete");
                filterChain.doFilter(request, response);
            } catch(CustomException e) {
                sendExceptionMessage(response, e);
            }
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
            response.setContentType("application/json;charset=UTF-8");
            ExceptionResponse exceptionResponse = ExceptionResponse.builder()
                            .exceptionCode(exception.getExceptionCode())
                            .message(exception.getMessage())
                            .status(exception.getExceptionCode().getHttpStatus().value())
                            .build();
            ObjectMapper objectMapper = new ObjectMapper();
            String message = objectMapper.writeValueAsString(exceptionResponse);

            PrintWriter writer = response.getWriter();
            writer.print(message);
            writer.flush();
            writer.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}

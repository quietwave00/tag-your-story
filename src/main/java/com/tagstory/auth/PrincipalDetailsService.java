package com.tagstory.auth;

import com.tagstory.entity.User;
import com.tagstory.exception.CustomException;
import com.tagstory.exception.ExceptionCode;
import com.tagstory.user.cache.CacheUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {

    private final CacheUserRepository cacheUserRepository;

    @Bean
    public AuthenticationManager authenticationManager() {
        return authentication -> authentication;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        log.info("PrincipalDetailsService Execute");
        User findUser = cacheUserRepository.findUserByUserId(Long.parseLong(userId)).orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        return new PrincipalDetails(findUser);
    }
}

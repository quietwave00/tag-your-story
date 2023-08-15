package com.tagstory.auth;

import com.tagstory.entity.User;
import com.tagstory.user.cache.CacheSpec;
import com.tagstory.user.cache.CacheUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(value = "user", key = "#userId")
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        log.info("PrincipalDetailsService Execute");
        User findUser = cacheUserRepository.findByUserId(Long.parseLong(userId), CacheSpec.USER);
        return new PrincipalDetails(findUser);
    }
}

package com.tagstory.api.auth;

import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.repository.UserRepository;
import com.tagstory.core.domain.user.repository.dto.CacheUser;
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

    private final UserRepository userRepository;

    @Bean
    public AuthenticationManager authenticationManager() {
        return authentication -> authentication;
    }

    @Override
    @Cacheable(value = "user", key = "#userId")
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        log.info("PrincipalDetailsService Execute");
        CacheUser cacheUser = userRepository.findCacheByUserId(Long.parseLong(userId), CacheSpec.USER);
        return new PrincipalDetails(CacheUser.toEntity(cacheUser));
    }
}

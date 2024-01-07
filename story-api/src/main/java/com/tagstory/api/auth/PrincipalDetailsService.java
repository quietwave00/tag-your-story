package com.tagstory.api.auth;

import com.tagstory.core.domain.user.service.UserService;
import com.tagstory.core.domain.user.service.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        log.info("PrincipalDetailsService Execute");
        User user = userService.getCacheByUserId(Long.valueOf(userId));
        return new PrincipalDetails(user.getUserId());
    }
}

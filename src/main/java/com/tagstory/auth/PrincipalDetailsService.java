package com.tagstory.auth;

import com.tagstory.entity.User;
import com.tagstory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
    public UserDetails loadUserByUsername(String userKey) throws UsernameNotFoundException {
        log.info("PrincipalDetailsService Execute");
        User findUser = userRepository.findByUserKey(userKey)
                .orElseThrow(() -> new UsernameNotFoundException("dd"));
        return new PrincipalDetails(findUser);
    }
}

package com.tagstory.oauth;

import com.tagstory.entity.User;
import com.tagstory.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /*
     * 사용자 정보를 가져오고, 정보를 토대로 회원가입 여부를 체크한다.
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        log.info("Oauth2UserService Execute");
        Map<String, Object> attributes = super.loadUser(request).getAttributes();
        isExistUser(attributes);
        System.out.println(request.getClientRegistration().getRegistrationId());
        return super.loadUser(request);
    }

    /*
     * 회원 정보가 존재하는지 확인하고, 존재하지 않을 경우 사용자를 생성한다.
     */
    private void isExistUser(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        String userKey = (String) attributes.get("sub");

        if (!userRepository.existsByEmailAndUserKey(email, userKey)) {
            User user = User.register(email, userKey);
            userRepository.save(user);
        }
    }
}

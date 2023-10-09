package com.tagstory.api.oauth;

import com.tagstory.api.auth.PrincipalDetails;
import com.tagstory.core.domain.user.Role;
import com.tagstory.core.domain.user.UserStatus;
import com.tagstory.core.domain.user.service.UserService;
import com.tagstory.core.domain.user.service.dto.response.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrincipalOauth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    /*
     * 사용자 정보를 가져오고, 정보를 토대로 회원가입 여부를 체크한다.
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        log.info("Oauth2UserService Execute");
        Map<String, Object> attributes = super.loadUser(request).getAttributes();

        User user = findUser(attributes);
        /* 유저가 가입된 상황 */
        if(Objects.nonNull(user)) {
            return new PrincipalDetails(user.getUserId(), user.getRole(), attributes);
        } else {
            /* 유저가 미가입된 상황 */
            user = savePendingUser(attributes);
            return new PrincipalDetails(user.getPendingUserId(), user.getRole(), attributes);
        }
    }

    /*
     * 가입된 유저 정보가 있는지 찾는다.
     */
    private User findUser(Map<String, Object> attributes) {
        String userKey = (String) attributes.get("sub");
        return userService.findByUserKey(userKey);
    }

    /*
     * 회원 정보가 존재하지 않을 경우 사용자를 임시로 저장한다.
     */
    public User savePendingUser(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        String userKey = (String) attributes.get("sub");

        User user = User.builder()
                .pendingUserId(UUID.randomUUID().toString())
                .email(email)
                .userKey(userKey)
                .userStatus(UserStatus.ACTIVE)
                .role(Role.ROLE_PENDING_USER)
                .build();

        return userService.saveCachedPendingUser(user);
    }
}

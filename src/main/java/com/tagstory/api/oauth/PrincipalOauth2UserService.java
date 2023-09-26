package com.tagstory.api.oauth;

import com.tagstory.api.auth.PrincipalDetails;
import com.tagstory.api.exception.CustomException;
import com.tagstory.api.exception.ExceptionCode;
import com.tagstory.core.config.CacheSpec;
import com.tagstory.core.domain.user.UserEntity;
import com.tagstory.core.domain.user.repository.UserRepository;
import com.tagstory.core.domain.user.repository.dto.CacheUser;
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
        if(isRegistered(attributes)) {
            UserEntity user = findUserByUserKey((String) attributes.get("sub"));
            return new PrincipalDetails(user.getUserId(), user.getRole(), attributes);

        } else {
            CacheUser cacheUser = register(attributes);
            return new PrincipalDetails(cacheUser.getTempId(), cacheUser.getRole(), attributes);
        }
    }


    /*
     * 회원 정보가 존재하는지 확인한다.
     */
    private boolean isRegistered(Map<String, Object> attributes) {
        String userKey = (String) attributes.get("sub");
        return userRepository.findByUserKey(userKey).isPresent();
    }

    /*
     * 회원 정보가 존재할 경우 해당 사용자의 아이디값을 반환해준다.
     */
    private UserEntity findUserByUserKey(String userKey) {
        return userRepository.findByUserKey(userKey).orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
    }


    /*
     * 회원 정보가 존재하지 않을 경우 사용자를 메모리에 임시로 생성한다.
     */
    public CacheUser register(Map<String, Object> attributes) {
        String email = (String) attributes.get("email");
        String userKey = (String) attributes.get("sub");
        UserEntity userEntity = UserEntity.register(userKey, email);
        return userRepository.savePendingUser(CacheUser.create(userEntity), CacheSpec.PENDING_USER);
    }
}

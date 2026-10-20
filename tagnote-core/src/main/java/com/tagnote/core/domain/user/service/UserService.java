package com.tagnote.core.domain.user.service;

import com.tagnote.core.exception.CustomException;
import com.tagnote.core.exception.ExceptionCode;
import com.tagnote.core.utils.jwt.JwtUtil;
import com.tagnote.core.common.CacheSpec;
import com.tagnote.core.domain.user.Role;
import com.tagnote.core.domain.user.UserStatus;
import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.AccountProvider;
import com.tagnote.core.domain.user.EmailNormalizer;
import com.tagnote.core.common.CommonRedisTemplate;
import com.tagnote.core.domain.user.repository.UserRepository;
import com.tagnote.core.domain.user.service.dto.command.ReissueAccessTokenCommand;
import com.tagnote.core.domain.user.service.dto.response.Token;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

import static com.tagnote.core.utils.jwt.JwtProperties.TOKEN_TYPE_REFRESH;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserService  {

    private final UserRepository userRepository;
    private final CommonRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    public Token reissueAccessToken(ReissueAccessTokenCommand reissueAccessTokenCommand) {
        String refreshToken = reissueAccessTokenCommand.getRefreshToken();
        Long userId = jwtUtil.getUserIdFromToken(refreshToken, TOKEN_TYPE_REFRESH);
        if (!refreshToken.equals(redisTemplate.get(userId, CacheSpec.REFRESH_TOKEN))) {
            throw new CustomException(ExceptionCode.INVALID_REFRESH_TOKEN);
        }
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.INVALID_REFRESH_TOKEN));
        if (user.getUserStatus() != UserStatus.ACTIVE || user.getRole() != Role.ROLE_USER) {
            throw new CustomException(ExceptionCode.INVALID_REFRESH_TOKEN);
        }
        String newAccessToken = jwtUtil.generateAccessToken(userId);
        return Token.onComplete(newAccessToken);
    }

    public Token reissueRefreshToken(final Long userId) {
        User user = getCacheByUserId(userId);
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUserId());
        redisTemplate.set(user.getUserId(), newRefreshToken, CacheSpec.REFRESH_TOKEN);
        return Token.onComplete(newRefreshToken);
    }

    public void logout(Long userId) {
        redisTemplate.delete(userId, CacheSpec.REFRESH_TOKEN);
        SecurityContextHolder.clearContext();
    }


    /*
     * 단일 메소드
     */
    public void saveCache(UserEntity userEntity) {
        userRepository.saveCache(userEntity.toUser(), CacheSpec.USER);
    }

    public User getCacheByUserId(Long userId) {
        // The 365-day cache cannot determine whether a user has since been suspended.
        return requireActiveUser(userId);
    }

    public User getCacheByUserKey(String userKey) {
        return Optional.ofNullable(userRepository.findCachedUserByUserKey(userKey, CacheSpec.USER))
                .orElseGet(() -> userRepository.findByUserKey(userKey)
                        .map(UserEntity::toUser)
                        .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND)));
    }

    public User requireActiveUser(Long userId) {
        UserEntity user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ExceptionCode.USER_NOT_FOUND));
        if (user.getUserStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ExceptionCode.ACCOUNT_WITHDRAWN);
        }
        if (user.getUserStatus() != UserStatus.ACTIVE || user.getRole() != Role.ROLE_USER) {
            throw new CustomException(ExceptionCode.USER_INACTIVE);
        }
        return user.toUser();
    }

    @Nullable
    public User findByUserId(Long userId) {
        return userRepository.findByUserId(userId)
                .map(UserEntity::toUser)
                .orElse(null);
    }

    @Nullable
    public User findByUserKey(String userKey) {
        return userRepository.findByUserKey(userKey)
                .map(UserEntity::toUser)
                .orElse(null);
    }

    @Nullable
    public User findGoogleBySubject(String subject) {
        return userRepository.findByProviderAndSubject(AccountProvider.GOOGLE, subject)
                .map(UserEntity::toUser).orElse(null);
    }

    @Nullable
    public User findByEmail(String email) {
        return userRepository.findByEmail(EmailNormalizer.normalize(email))
                .map(UserEntity::toUser).orElse(null);
    }

    public boolean hasCachedUserInfo(Long userId) {
        return Objects.nonNull(userRepository.findCachedUserByUserId(userId, CacheSpec.USER));
    }
}

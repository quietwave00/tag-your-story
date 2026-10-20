package com.tagnote.core.domain.user.repository;

import com.tagnote.core.domain.user.UserEntity;
import com.tagnote.core.domain.user.AccountProvider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<UserEntity, Long>, CacheUserRepository{
    boolean existsByEmailAndUserKey(String email, String userKey);
    Optional<UserEntity> findByUserKey(String userKey);
    Optional<UserEntity> findByProviderAndSubject(AccountProvider provider, String subject);
    Optional<UserEntity> findByEmail(String normalizedEmail);
    boolean existsByNickname(String nickname);
    Optional<UserEntity> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserEntity u where u.userId = :userId")
    Optional<UserEntity> findByUserIdForUpdate(@Param("userId") Long userId);
}

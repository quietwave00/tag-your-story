package com.tagstory.core.domain.user.repository;

import com.tagstory.core.domain.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<UserEntity, Long>, CacheUserRepository{
    boolean existsByEmailAndUserKey(String email, String userKey);
    Optional<UserEntity> findByUserKey(String userKey);
    Optional<UserEntity> findByUserId(Long userId);
}

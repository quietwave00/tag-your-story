package com.tagstory.core.domain.user.repository;

import com.tagstory.core.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long>, CacheUserRepository{
    boolean existsByEmailAndUserKey(String email, String userKey);
    Optional<User> findByUserKey(String userKey);
    Optional<User> findByUserId(Long userId);
}

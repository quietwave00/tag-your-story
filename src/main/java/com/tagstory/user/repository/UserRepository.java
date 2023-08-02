package com.tagstory.user.repository;

import com.tagstory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailAndUserKey(String email, String userKey);
    Optional<User> findByUserKey(String userKey);
    Optional<User> findByUserId(Long userId);
}

package com.tagstory.user.repository;

import com.tagstory.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailAndUserKey(String email, String userKey);
    User findByUserKey(String userKey);
}

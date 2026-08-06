package com.tagnote.core.domain.usertag.repository;

import com.tagnote.core.domain.usertag.UserTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTagRepository extends JpaRepository<UserTagEntity, Long> {

    Optional<UserTagEntity> findByName(String userTagStr);

    List<UserTagEntity> findAllByNameIn(List<String> userTagNameList);
}

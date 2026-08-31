package com.tagnote.core.domain.usertag.repository;

import com.tagnote.core.domain.usertag.UserTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTagRepository extends JpaRepository<UserTagEntity, Long> {

    List<UserTagEntity> findAllByOwner_UserIdAndNormalizedNameIn(Long ownerUserId, List<String> normalizedNames);
}

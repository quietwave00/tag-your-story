package com.tagstory.core.domain.hashtag.repository;

import com.tagstory.core.domain.hashtag.HashtagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HashtagRepository extends JpaRepository<HashtagEntity, Long> {

    Optional<HashtagEntity> findByName(String hashtagStr);

    List<HashtagEntity> findAllByNameIn(List<String> hashtagNameList);
}

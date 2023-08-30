package com.tagstory.core.domain.hashtag.repository;

import com.tagstory.core.domain.hashtag.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

}

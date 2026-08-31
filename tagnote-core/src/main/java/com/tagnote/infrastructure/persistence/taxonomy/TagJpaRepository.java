package com.tagnote.infrastructure.persistence.taxonomy;

import com.tagnote.domain.taxonomy.tag.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TagJpaRepository extends JpaRepository<TagEntity, Long> {

    Optional<TagEntity> findBySlug(String slug);

    @Query("select tag from TagEntity tag left join fetch tag.mergedIntoTag")
    List<TagEntity> findAllWithMergeTarget();
}

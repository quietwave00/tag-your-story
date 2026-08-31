package com.tagnote.infrastructure.persistence.resolution;

import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.resolution.SubjectTagResolvedEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubjectTagResolvedJpaRepository extends JpaRepository<SubjectTagResolvedEntity, Long> {

    @Query("""
            select resolved
            from SubjectTagResolvedEntity resolved
            join fetch resolved.tag
            where resolved.subjectType = :subjectType
              and resolved.subjectId = :subjectId
            """)
    List<SubjectTagResolvedEntity> findAllBySubjectWithTag(
            @Param("subjectType") SubjectType subjectType,
            @Param("subjectId") long subjectId
    );
}

package com.tagnote.infrastructure.persistence.resolution;

import com.tagnote.domain.enrichment.subject.SubjectType;
import com.tagnote.domain.resolution.ResolvedStatus;
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

    boolean existsBySubjectTypeAndSubjectId(SubjectType subjectType, long subjectId);

    @Query("""
            select resolved
            from SubjectTagResolvedEntity resolved
            join fetch resolved.tag tag
            where resolved.subjectType = :subjectType
              and resolved.subjectId = :subjectId
              and resolved.status <> :hiddenStatus
            order by resolved.score desc, tag.tagId asc
            """)
    List<SubjectTagResolvedEntity> findVisibleBySubjectWithTag(
            @Param("subjectType") SubjectType subjectType,
            @Param("subjectId") long subjectId,
            @Param("hiddenStatus") ResolvedStatus hiddenStatus
    );
}

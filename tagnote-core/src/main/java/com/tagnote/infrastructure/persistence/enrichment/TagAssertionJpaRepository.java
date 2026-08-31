package com.tagnote.infrastructure.persistence.enrichment;

import com.tagnote.domain.enrichment.assertion.AssertionSource;
import com.tagnote.domain.enrichment.assertion.EvidenceType;
import com.tagnote.domain.enrichment.assertion.TagAssertionEntity;
import com.tagnote.domain.enrichment.subject.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TagAssertionJpaRepository extends JpaRepository<TagAssertionEntity, Long> {

    @Query("""
            select assertion
            from TagAssertionEntity assertion
            join fetch assertion.tag
            where assertion.subjectType = :subjectType
              and assertion.subjectId = :subjectId
              and assertion.status = com.tagnote.domain.enrichment.assertion.AssertionStatus.APPROVED
              and assertion.inheritedFromAssertion is null
            """)
    List<TagAssertionEntity> findApprovedDirectBySubject(
            @Param("subjectType") SubjectType subjectType,
            @Param("subjectId") long subjectId
    );

    @Query("""
            select assertion
            from TagAssertionEntity assertion
            join fetch assertion.tag
            where assertion.subjectType = :subjectType
              and assertion.subjectId = :subjectId
              and assertion.status = com.tagnote.domain.enrichment.assertion.AssertionStatus.APPROVED
            """)
    List<TagAssertionEntity> findApprovedBySubject(
            @Param("subjectType") SubjectType subjectType,
            @Param("subjectId") long subjectId
    );

    @Query("""
            select assertion
            from TagAssertionEntity assertion
            join fetch assertion.tag
            join fetch assertion.inheritedFromAssertion parent
            join fetch parent.tag
            where assertion.subjectType = :subjectType
              and assertion.subjectId = :subjectId
              and assertion.inheritedFromAssertion is not null
            """)
    List<TagAssertionEntity> findInheritedBySubject(
            @Param("subjectType") SubjectType subjectType,
            @Param("subjectId") long subjectId
    );

    @Query("""
            select assertion
            from TagAssertionEntity assertion
            join fetch assertion.tag
            where assertion.subjectType = :subjectType
              and assertion.subjectId = :subjectId
              and assertion.tag.tagId in :tagIds
              and assertion.source in :sources
              and assertion.evidenceType in :evidenceTypes
            """)
    List<TagAssertionEntity> findExistingForInputs(
            @Param("subjectType") SubjectType subjectType,
            @Param("subjectId") long subjectId,
            @Param("tagIds") Collection<Long> tagIds,
            @Param("sources") Collection<AssertionSource> sources,
            @Param("evidenceTypes") Collection<EvidenceType> evidenceTypes
    );
}

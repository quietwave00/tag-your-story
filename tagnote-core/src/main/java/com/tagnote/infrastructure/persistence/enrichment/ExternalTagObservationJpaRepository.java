package com.tagnote.infrastructure.persistence.enrichment;

import com.tagnote.domain.enrichment.observation.ExternalTagObservationEntity;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import com.tagnote.domain.enrichment.subject.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ExternalTagObservationJpaRepository
        extends JpaRepository<ExternalTagObservationEntity, Long> {

    @Query("""
            select observation
            from ExternalTagObservationEntity observation
            left join fetch observation.matchedTag
            where observation.subjectType = :subjectType
              and observation.subjectId = :subjectId
              and observation.source in :sources
              and observation.normalizedName in :normalizedNames
              and observation.externalRef in :externalRefs
            """)
    List<ExternalTagObservationEntity> findExistingForInputs(
            @Param("subjectType") SubjectType subjectType,
            @Param("subjectId") long subjectId,
            @Param("sources") Collection<ExternalTagSource> sources,
            @Param("normalizedNames") Collection<String> normalizedNames,
            @Param("externalRefs") Collection<String> externalRefs
    );
}

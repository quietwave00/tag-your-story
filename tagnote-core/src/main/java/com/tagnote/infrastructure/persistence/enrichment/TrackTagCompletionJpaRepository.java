package com.tagnote.infrastructure.persistence.enrichment;

import com.tagnote.domain.enrichment.observation.TrackTagCompletionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackTagCompletionJpaRepository extends JpaRepository<TrackTagCompletionEntity, Long> {
}

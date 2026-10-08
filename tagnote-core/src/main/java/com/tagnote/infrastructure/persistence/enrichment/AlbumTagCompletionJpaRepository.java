package com.tagnote.infrastructure.persistence.enrichment;

import com.tagnote.domain.enrichment.observation.AlbumTagCompletionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumTagCompletionJpaRepository extends JpaRepository<AlbumTagCompletionEntity, Long> {
}

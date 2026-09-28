package com.tagnote.application.enrichment;

import com.tagnote.domain.enrichment.observation.TrackTagCompletionEntity;
import com.tagnote.infrastructure.persistence.enrichment.TrackTagCompletionJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackTagCompletionService {
    private final TrackTagCompletionJpaRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public boolean completed(long trackId) {
        return repository.existsById(trackId);
    }

    @Transactional
    public void complete(long trackId) {
        entityManager.persist(new TrackTagCompletionEntity(trackId));
        entityManager.flush();
    }
}

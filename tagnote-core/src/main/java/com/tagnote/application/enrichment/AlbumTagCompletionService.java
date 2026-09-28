package com.tagnote.application.enrichment;

import com.tagnote.domain.enrichment.observation.AlbumTagCompletionEntity;
import com.tagnote.infrastructure.persistence.enrichment.AlbumTagCompletionJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlbumTagCompletionService {
    private final AlbumTagCompletionJpaRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public boolean completed(long albumId) {
        return repository.existsById(albumId);
    }

    @Transactional
    public void complete(long albumId) {
        entityManager.persist(new AlbumTagCompletionEntity(albumId));
        entityManager.flush();
    }
}

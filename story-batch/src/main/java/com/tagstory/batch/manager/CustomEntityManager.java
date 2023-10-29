package com.tagstory.batch.manager;

import lombok.RequiredArgsConstructor;

import javax.persistence.EntityManager;

@RequiredArgsConstructor
public class CustomEntityManager implements AutoCloseable {

    private final EntityManager entityManager;

    @Override
    public void close() {
        if (entityManager != null) {
            entityManager.close();
        }
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }
}

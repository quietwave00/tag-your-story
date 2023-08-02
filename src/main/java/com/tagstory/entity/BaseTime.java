package com.tagstory.entity;

import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@Getter
public abstract class BaseTime {
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @PrePersist
    private void whenCreated() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = createdAt;
    }

    @PreUpdate
    private void whenUpdated() {
        this.updatedAt = LocalDateTime.now();
    }
}

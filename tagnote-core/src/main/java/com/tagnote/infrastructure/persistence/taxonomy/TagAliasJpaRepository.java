package com.tagnote.infrastructure.persistence.taxonomy;

import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TagAliasJpaRepository extends JpaRepository<TagAliasEntity, Long> {

    @Query("""
            select alias
            from TagAliasEntity alias
            join fetch alias.tag
            where alias.normalizedAlias in :normalizedAliases
              and alias.status = com.tagnote.domain.taxonomy.alias.AliasStatus.APPROVED
            """)
    List<TagAliasEntity> findApprovedByNormalizedAliases(
            @Param("normalizedAliases") Collection<String> normalizedAliases
    );
}

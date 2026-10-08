package com.tagnote.infrastructure.persistence.taxonomy;

import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@TestConfiguration
@EntityScan(basePackageClasses = {TagEntity.class, TagAliasEntity.class})
@EnableJpaRepositories(basePackageClasses = {TagJpaRepository.class, TagAliasJpaRepository.class})
class TaxonomyJpaTestConfiguration {
}

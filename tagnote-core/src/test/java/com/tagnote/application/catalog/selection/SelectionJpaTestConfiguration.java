package com.tagnote.application.catalog.selection;

import com.tagnote.domain.catalog.album.AlbumArtistEntity;
import com.tagnote.domain.catalog.album.AlbumEntity;
import com.tagnote.domain.catalog.artist.ArtistEntity;
import com.tagnote.domain.catalog.track.TrackArtistEntity;
import com.tagnote.domain.catalog.track.TrackEntity;
import com.tagnote.domain.enrichment.assertion.TagAssertionEntity;
import com.tagnote.domain.enrichment.observation.ExternalTagObservationEntity;
import com.tagnote.domain.resolution.SubjectTagResolvedEntity;
import com.tagnote.domain.taxonomy.alias.TagAliasEntity;
import com.tagnote.domain.taxonomy.tag.TagEntity;
import com.tagnote.infrastructure.persistence.catalog.TrackJpaRepository;
import com.tagnote.infrastructure.persistence.enrichment.TagAssertionJpaRepository;
import com.tagnote.infrastructure.persistence.resolution.SubjectTagResolvedJpaRepository;
import com.tagnote.infrastructure.persistence.taxonomy.TagJpaRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@TestConfiguration
@EntityScan(basePackageClasses = {
        ArtistEntity.class,
        AlbumEntity.class,
        AlbumArtistEntity.class,
        TrackEntity.class,
        TrackArtistEntity.class,
        TagEntity.class,
        TagAliasEntity.class,
        ExternalTagObservationEntity.class,
        TagAssertionEntity.class,
        SubjectTagResolvedEntity.class
})
@EnableJpaRepositories(basePackageClasses = {
        TrackJpaRepository.class,
        TagJpaRepository.class,
        TagAssertionJpaRepository.class,
        SubjectTagResolvedJpaRepository.class
})
class SelectionJpaTestConfiguration {

    @Bean
    static ImportQueryProbeDataSourcePostProcessor importQueryProbeDataSourcePostProcessor() {
        return new ImportQueryProbeDataSourcePostProcessor();
    }

    @Bean
    ImportQueryProbe importQueryProbe(ImportQueryProbeDataSourcePostProcessor postProcessor) {
        return postProcessor.probe();
    }

    static final class ImportQueryProbeDataSourcePostProcessor implements BeanPostProcessor {

        private final ImportQueryProbe probe = new ImportQueryProbe();

        ImportQueryProbe probe() {
            return probe;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (!(bean instanceof DataSource dataSource)) {
                return bean;
            }
            return ProxyDataSourceBuilder.create(dataSource)
                    .name("track-import-query-test")
                    .listener(probe)
                    .build();
        }
    }

    static final class ImportQueryProbe implements QueryExecutionListener {

        private final List<String> executions = new ArrayList<>();

        @Override
        public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
        }

        @Override
        public synchronized void afterQuery(ExecutionInfo executionInfo, List<QueryInfo> queryInfoList) {
            queryInfoList.stream()
                    .map(QueryInfo::getQuery)
                    .map(this::normalize)
                    .forEach(executions::add);
        }

        synchronized void reset() {
            executions.clear();
        }

        synchronized int executionCount() {
            return executions.size();
        }

        synchronized long selectCountContaining(String fragment) {
            String expected = fragment.toLowerCase(Locale.ROOT);
            return executions.stream()
                    .filter(sql -> sql.startsWith("select") || sql.contains("*/ select"))
                    .filter(sql -> sql.contains(expected))
                    .count();
        }

        private String normalize(String sql) {
            return sql.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        }
    }
}

package com.tagnote.infrastructure.persistence.enrichment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tagnote.application.enrichment.diagnostics.EnrichmentDiagnostics;
import com.tagnote.application.enrichment.model.EnrichmentDeadline;
import com.tagnote.application.enrichment.model.ExternalTagInput;
import com.tagnote.application.enrichment.model.ProviderEnrichmentStatus;
import com.tagnote.application.enrichment.port.AlbumProviderEvidenceRepository;
import com.tagnote.domain.enrichment.observation.ExternalTagSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.io.StringReader;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AlbumProviderEvidenceStore implements AlbumProviderEvidenceRepository {
    private static final long LEASE_MS = 300_000;
    private static final TypeReference<List<ExternalTagInput>> INPUTS = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;

    public AlbumProviderEvidenceStore(JdbcTemplate jdbc, ObjectMapper json,
                                      PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.json = json;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public Claim acquire(long albumId, ExternalTagSource source, EnrichmentDeadline deadline) {
        while (deadline.remainingMillis() > 0 && !Thread.currentThread().isInterrupted()) {
            Claim claim;
            try {
                claim = transaction.execute(status -> claimOnce(albumId, source));
            } catch (DuplicateKeyException concurrentInsert) {
                // A different instance inserted the unique branch first.
                continue;
            }
            if (claim != null) return claim;
            try {
                Thread.sleep(Math.min(50, Math.max(1, deadline.remainingMillis())));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return new Claim(ProviderEnrichmentStatus.TIMEOUT, List.of(), null, false);
    }

    private Claim claimOnce(long albumId, ExternalTagSource source) {
        List<Row> rows = jdbc.query("""
                select status, raw_tags, lease_token, lease_until
                  from album_provider_evidence
                 where album_id = ? and source = ? for update
                """, (rs, index) -> new Row(rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getTimestamp(4) == null ? null : rs.getTimestamp(4).toInstant()), albumId, source.name());
        if (rows.isEmpty()) {
            String token = UUID.randomUUID().toString();
            jdbc.update("""
                    insert into album_provider_evidence
                    (album_id, source, status, raw_tags, lease_token, lease_until, completed_at)
                    values (?, ?, 'IN_PROGRESS', null, ?, ?, null)
                    """, albumId, source.name(), token, Timestamp.from(Instant.now().plusMillis(LEASE_MS)));
            return new Claim(null, List.of(), token, false);
        }
        Row row = rows.get(0);
        if (isReusable(row.status())) {
            return new Claim(ProviderEnrichmentStatus.valueOf(row.status()), decode(row.rawTags()), null, true);
        }
        if ("IN_PROGRESS".equals(row.status()) && row.leaseUntil() != null
                && row.leaseUntil().isAfter(Instant.now())) return null;
        String token = UUID.randomUUID().toString();
        jdbc.update("""
                update album_provider_evidence
                   set status = 'IN_PROGRESS', raw_tags = null, lease_token = ?, lease_until = ?, completed_at = null
                 where album_id = ? and source = ?
                """, token, Timestamp.from(Instant.now().plusMillis(LEASE_MS)), albumId, source.name());
        return new Claim(null, List.of(), token, false);
    }

    @Override
    public boolean owns(long albumId, ExternalTagSource source, String token) {
        if (token == null) return false;
        Integer count = jdbc.queryForObject("""
                select count(*) from album_provider_evidence
                 where album_id = ? and source = ? and status = 'IN_PROGRESS' and lease_token = ?
                   and lease_until > ?
                """, Integer.class, albumId, source.name(), token, Timestamp.from(Instant.now()));
        return count != null && count == 1;
    }

    @Override
    public boolean complete(long albumId, ExternalTagSource source, String token,
                         ProviderEnrichmentStatus status, List<ExternalTagInput> tags) {
        if (token == null || !isReusable(status.name())) return false;
        String payload = encode(tags);
        int changed = jdbc.update("""
                update album_provider_evidence
                   set status = ?, raw_tags = ?, lease_token = null, lease_until = null, completed_at = ?
                 where album_id = ? and source = ? and status = 'IN_PROGRESS' and lease_token = ?
                   and lease_until > ?
                """, statement -> {
            statement.setString(1, status.name());
            statement.setCharacterStream(2, new StringReader(payload), payload.length());
            statement.setTimestamp(3, Timestamp.from(Instant.now()));
            statement.setLong(4, albumId);
            statement.setString(5, source.name());
            statement.setString(6, token);
            statement.setTimestamp(7, Timestamp.from(Instant.now()));
        });
        EnrichmentDiagnostics.event("album_evidence_committed", java.util.Map.of(
                "provider", source.name(), "albumId", albumId, "status", status.name(),
                "tagCount", tags.size(), "committed", changed == 1));
        return changed == 1;
    }

    @Override
    public void release(long albumId, ExternalTagSource source, String token, ProviderEnrichmentStatus status) {
        if (token == null) return;
        jdbc.update("""
                update album_provider_evidence
                   set status = ?, raw_tags = null, lease_token = null, lease_until = null, completed_at = null
                 where album_id = ? and source = ? and status = 'IN_PROGRESS' and lease_token = ?
                """, status.name(), albumId, source.name(), token);
    }

    private boolean isReusable(String status) {
        return "SUCCESS".equals(status) || "EMPTY".equals(status) || "NOT_FOUND".equals(status);
    }

    private String encode(List<ExternalTagInput> tags) {
        try {
            return json.writeValueAsString(tags);
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Could not serialize Album provider evidence", failure);
        }
    }

    private List<ExternalTagInput> decode(String payload) {
        if (payload == null) return List.of();
        try {
            return List.copyOf(json.readValue(payload, INPUTS));
        } catch (JsonProcessingException failure) {
            throw new IllegalStateException("Could not deserialize Album provider evidence", failure);
        }
    }

    private record Row(String status, String rawTags, String leaseToken, Instant leaseUntil) {}
}

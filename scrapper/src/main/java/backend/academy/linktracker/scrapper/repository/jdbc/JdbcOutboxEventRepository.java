package backend.academy.linktracker.scrapper.repository.jdbc;

import backend.academy.linktracker.scrapper.entity.OutboxEventEntity;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JdbcOutboxEventRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<OutboxEventEntity> ROW_MAPPER = (rs, rowNum) -> {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setId(rs.getLong("id"));
        entity.setAggregateId(rs.getLong("aggregate_id"));
        entity.setEventType(rs.getString("event_type"));
        entity.setPayload(rs.getString("payload"));
        entity.setStatus(OutboxEventEntity.OutboxStatus.valueOf(rs.getString("status")));
        entity.setCreatedAt(rs.getObject("created_at", OffsetDateTime.class));
        entity.setProcessedAt(rs.getObject("processed_at", OffsetDateTime.class));
        entity.setRetryCount(rs.getInt("retry_count"));
        entity.setErrorMessage(rs.getString("error_message"));
        return entity;
    };

    public OutboxEventEntity save(OutboxEventEntity entity) {
        String sql = """
            INSERT INTO outbox_events (aggregate_id, event_type, payload, status, created_at, retry_count)
            VALUES (?, ?, ?, ?::VARCHAR, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, entity.getAggregateId());
            ps.setString(2, entity.getEventType());
            ps.setString(3, entity.getPayload());
            ps.setString(4, entity.getStatus().name());
            ps.setTimestamp(5, Timestamp.from(entity.getCreatedAt().toInstant()));
            ps.setInt(6, entity.getRetryCount());
            return ps;
        }, keyHolder);

        entity.setId(keyHolder.getKey().longValue());
        return entity;
    }

    public List<OutboxEventEntity> findPendingEvents() {
        String sql = "SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public void updateStatus(Long id, OutboxEventEntity.OutboxStatus status) {
        String sql = "UPDATE outbox_events SET status = ?::VARCHAR WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), id);
    }

    public void updateProcessed(Long id, OffsetDateTime processedAt) {
        String sql = "UPDATE outbox_events SET status = 'PROCESSED', processed_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, Timestamp.from(processedAt.toInstant()), id);
    }

    public void updateFailed(Long id, String errorMessage, int retryCount) {
        String sql = "UPDATE outbox_events SET status = 'FAILED', error_message = ?, retry_count = ? WHERE id = ?";
        jdbcTemplate.update(sql, errorMessage, retryCount, id);
    }
}

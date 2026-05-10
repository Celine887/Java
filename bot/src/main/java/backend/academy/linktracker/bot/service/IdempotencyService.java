package backend.academy.linktracker.bot.service;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app", name = "idempotency.enabled", havingValue = "true", matchIfMissing = true)
public class IdempotencyService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public boolean isProcessed(Long messageId) {
        String sql = "SELECT COUNT(*) FROM processed_messages WHERE message_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, messageId);
        return count != null && count > 0;
    }

    @Transactional
    public void markAsProcessed(Long messageId) {
        String sql = "INSERT INTO processed_messages (message_id, processed_at) VALUES (?, ?) ON CONFLICT DO NOTHING";
        jdbcTemplate.update(sql, messageId, Timestamp.from(OffsetDateTime.now().toInstant()));
        log.debug("Marked message as processed: id={}", messageId);
    }
}

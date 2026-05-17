package backend.academy.linktracker.scrapper.repository.jdbc;

import backend.academy.linktracker.scrapper.model.Chat;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "jdbc")
public class JdbcChatRepositoryImpl implements ChatRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcChatRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void save(Chat chat) {
        jdbcTemplate.update(
            "INSERT INTO chats (id, created_at) VALUES (?, CURRENT_TIMESTAMP) ON CONFLICT (id) DO NOTHING",
            chat.id()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findById(Long id) {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT id, created_at FROM chats WHERE id = ?",
                (rs, rowNum) -> Optional.of(new Chat(
                    rs.getLong("id"),
                    rs.getTimestamp("created_at").toInstant()
                )),
                id
            );
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chats WHERE id = ?",
            Integer.class,
            id
        );
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM chats WHERE id = ?", id);
    }
}

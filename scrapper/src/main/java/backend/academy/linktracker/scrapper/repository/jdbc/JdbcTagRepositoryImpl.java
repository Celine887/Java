package backend.academy.linktracker.scrapper.repository.jdbc;

import backend.academy.linktracker.scrapper.repository.TagRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "jdbc")
public class JdbcTagRepositoryImpl implements TagRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcTagRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void save(String tagName) {
        jdbcTemplate.update(
                "INSERT INTO tags (name, created_at) VALUES (?, CURRENT_TIMESTAMP) ON CONFLICT (name) DO NOTHING",
                tagName);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findById(Long id) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT name FROM tags WHERE id = ?", (rs, rowNum) -> Optional.of(rs.getString("name")), id);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findByName(String name) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT name FROM tags WHERE name = ?", (rs, rowNum) -> Optional.of(rs.getString("name")), name);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAll() {
        return jdbcTemplate.query("SELECT name FROM tags ORDER BY name", (rs, rowNum) -> rs.getString("name"));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM tags WHERE id = ?", id);
    }

    @Override
    @Transactional
    public void deleteByName(String name) {
        jdbcTemplate.update("DELETE FROM tags WHERE name = ?", name);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tags WHERE name = ?", Integer.class, name);
        return count != null && count > 0;
    }
}

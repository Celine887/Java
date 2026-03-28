package backend.academy.linktracker.scrapper.repository.jdbc;

import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "jdbc")
public class JdbcLinkRepositoryImpl implements LinkRepository {
    private final JdbcTemplate jdbcTemplate;

    public JdbcLinkRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void save(Long chatId, Link link) {
        Long linkId = findOrCreateLink(link);
        createLinkChatAssociation(linkId, chatId);
        if (link.tags() != null && !link.tags().isEmpty()) {
            saveTags(linkId, link.tags());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Link> findByChatIdAndUrl(Long chatId, String url) {
        try {
            return jdbcTemplate.queryForObject("""
                SELECT DISTINCT l.url, l.last_updated_at, l.last_checked_at
                FROM links l
                JOIN link_chat lc ON l.id = lc.link_id
                WHERE lc.chat_id = ? AND l.url = ?
                """, (rs, rowNum) -> Optional.of(mapRowToLink(rs)), chatId, url);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Link> findAllByChatId(Long chatId) {
        return jdbcTemplate.query("""
            SELECT DISTINCT l.url, l.last_updated_at, l.last_checked_at
            FROM links l
            JOIN link_chat lc ON l.id = lc.link_id
            WHERE lc.chat_id = ?
            LIMIT 1000
            """, (rs, rowNum) -> mapRowToLink(rs), chatId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Link> findAllByChatIdAndTag(Long chatId, String tag) {
        return jdbcTemplate.query("""
            SELECT DISTINCT l.url, l.last_updated_at, l.last_checked_at
            FROM links l
            JOIN link_chat lc ON l.id = lc.link_id
            JOIN link_tag lt ON l.id = lt.link_id
            JOIN tags t ON lt.tag_id = t.id
            WHERE lc.chat_id = ? AND t.name = ?
            LIMIT 1000
            """, (rs, rowNum) -> mapRowToLink(rs), chatId, tag);
    }

    @Override
    @Transactional
    public void deleteByChatIdAndUrl(Long chatId, String url) {
        Long linkId = jdbcTemplate.queryForObject("SELECT id FROM links WHERE url = ?", Long.class, url);

        if (linkId != null) {
            jdbcTemplate.update("DELETE FROM link_chat WHERE link_id = ? AND chat_id = ?", linkId, chatId);

            Integer remainingChats = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM link_chat WHERE link_id = ?", Integer.class, linkId);

            if (remainingChats != null && remainingChats == 0) {
                jdbcTemplate.update("DELETE FROM link_tag WHERE link_id = ?", linkId);
                jdbcTemplate.update("DELETE FROM links WHERE id = ?", linkId);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Link> findAllLinks() {
        return jdbcTemplate.query("""
            SELECT DISTINCT url, last_updated_at, last_checked_at
            FROM links
            LIMIT 10000
            """, (rs, rowNum) -> mapRowToLink(rs));
    }

    @Override
    @Transactional
    public void updateLink(Link link) {
        jdbcTemplate.update(
                """
            UPDATE links
            SET last_updated_at = ?, last_checked_at = ?
            WHERE url = ?
            """,
                link.lastUpdatedAt() != null ? Timestamp.from(link.lastUpdatedAt()) : null,
                Timestamp.from(link.lastCheckedAt()),
                link.url());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findChatIdsByUrl(String url) {
        return jdbcTemplate.query("""
            SELECT lc.chat_id
            FROM link_chat lc
            JOIN links l ON lc.link_id = l.id
            WHERE l.url = ?
            """, (rs, rowNum) -> rs.getLong("chat_id"), url);
    }

    private Long findOrCreateLink(Link link) {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM links WHERE url = ?", Long.class, link.url());
        } catch (EmptyResultDataAccessException e) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(
                    connection -> {
                        var ps = connection.prepareStatement(
                                "INSERT INTO links (url, last_updated_at, last_checked_at, created_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                                new String[] {"id"});
                        ps.setString(1, link.url());
                        ps.setTimestamp(2, link.lastUpdatedAt() != null ? Timestamp.from(link.lastUpdatedAt()) : null);
                        ps.setTimestamp(3, Timestamp.from(link.lastCheckedAt()));
                        return ps;
                    },
                    keyHolder);
            return keyHolder.getKey().longValue();
        }
    }

    private void createLinkChatAssociation(Long linkId, Long chatId) {
        jdbcTemplate.update(
                "INSERT INTO link_chat (link_id, chat_id) VALUES (?, ?) ON CONFLICT DO NOTHING", linkId, chatId);
    }

    private void saveTags(Long linkId, java.util.Set<String> tags) {
        for (String tagName : tags) {
            Long tagId = findOrCreateTag(tagName);
            jdbcTemplate.update(
                    "INSERT INTO link_tag (link_id, tag_id) VALUES (?, ?) ON CONFLICT DO NOTHING", linkId, tagId);
        }
    }

    private Long findOrCreateTag(String tagName) {
        try {
            return jdbcTemplate.queryForObject("SELECT id FROM tags WHERE name = ?", Long.class, tagName);
        } catch (EmptyResultDataAccessException e) {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(
                    connection -> {
                        var ps = connection.prepareStatement(
                                "INSERT INTO tags (name, created_at) VALUES (?, CURRENT_TIMESTAMP)",
                                new String[] {"id"});
                        ps.setString(1, tagName);
                        return ps;
                    },
                    keyHolder);
            return keyHolder.getKey().longValue();
        }
    }

    private Link mapRowToLink(ResultSet rs) throws SQLException {
        String url = rs.getString("url");
        Timestamp lastUpdatedTs = rs.getTimestamp("last_updated_at");
        Timestamp lastCheckedTs = rs.getTimestamp("last_checked_at");

        java.time.Instant lastUpdatedAt = lastUpdatedTs != null ? lastUpdatedTs.toInstant() : null;
        java.time.Instant lastCheckedAt = lastCheckedTs.toInstant();

        java.util.Set<String> tags = new java.util.HashSet<>(findTagsByUrl(url));
        return new Link(url, tags, lastCheckedAt, lastUpdatedAt);
    }

    private List<String> findTagsByUrl(String url) {
        return jdbcTemplate.query("""
            SELECT t.name
            FROM tags t
            JOIN link_tag lt ON t.id = lt.tag_id
            JOIN links l ON lt.link_id = l.id
            WHERE l.url = ?
            """, (rs, rowNum) -> rs.getString("name"), url);
    }
}

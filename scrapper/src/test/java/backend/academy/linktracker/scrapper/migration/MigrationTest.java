package backend.academy.linktracker.scrapper.migration;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class MigrationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyMigrationsSuccessfully() {
        // Verify all tables exist
        assertTableExists("chats");
        assertTableExists("links");
        assertTableExists("link_chat");
        assertTableExists("tags");
        assertTableExists("link_tag");
        assertTableExists("databasechangelog");
        assertTableExists("databasechangeloglock");
    }

    @Test
    void shouldHaveCorrectIndexes() {
        assertIndexExists("idx_links_url");
        assertIndexExists("idx_links_last_checked_at");
        assertIndexExists("idx_link_chat_chat_id");
        assertIndexExists("idx_link_chat_link_id");
        assertIndexExists("idx_link_tag_link_id");
        assertIndexExists("idx_link_tag_tag_id");
        assertIndexExists("idx_tags_name");
    }

    @Test
    void shouldHaveCorrectConstraints() {
        // Check primary keys
        assertPrimaryKeyExists("chats", "chats_pkey");
        assertPrimaryKeyExists("links", "links_pkey");
        assertPrimaryKeyExists("tags", "tags_pkey");

        // Check foreign keys
        assertForeignKeyExists("link_chat", "link_id");
        assertForeignKeyExists("link_chat", "chat_id");
        assertForeignKeyExists("link_tag", "link_id");
        assertForeignKeyExists("link_tag", "tag_id");
    }

    private void assertTableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?", Integer.class, tableName);
        assertThat(count).isEqualTo(1);
    }

    private void assertIndexExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE indexname = ?", Integer.class, indexName);
        assertThat(count).isEqualTo(1);
    }

    private void assertPrimaryKeyExists(String tableName, String constraintName) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.table_constraints
            WHERE table_name = ? AND constraint_type = 'PRIMARY KEY'
            """, Integer.class, tableName);
        assertThat(count).isGreaterThan(0);
    }

    private void assertForeignKeyExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM information_schema.key_column_usage
            WHERE table_name = ? AND column_name = ?
            """, Integer.class, tableName, columnName);
        assertThat(count).isGreaterThan(0);
    }
}

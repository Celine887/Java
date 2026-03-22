package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.BaseIntegrationTest;
import backend.academy.linktracker.scrapper.model.Chat;
import backend.academy.linktracker.scrapper.model.Link;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(JpaTestConfiguration.class)
class JpaRepositoryTest extends BaseIntegrationTest {

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("app.scheduler.enabled", () -> "false");
    }

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE link_tag, link_chat, tags, links, chats CASCADE");
    }

    @Test
    void shouldSaveAndFindChat() {
        Chat chat = new Chat(123L, java.time.Instant.now());
        chatRepository.save(chat);

        var found = chatRepository.findById(123L);
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(123L);
    }

    @Test
    void shouldCheckChatExists() {
        Chat chat = new Chat(456L, java.time.Instant.now());
        chatRepository.save(chat);

        assertThat(chatRepository.existsById(456L)).isTrue();
        assertThat(chatRepository.existsById(999L)).isFalse();
    }

    @Test
    void shouldDeleteChat() {
        Chat chat = new Chat(789L, java.time.Instant.now());
        chatRepository.save(chat);
        chatRepository.deleteById(789L);

        assertThat(chatRepository.existsById(789L)).isFalse();
    }

    @Test
    void shouldSaveAndFindLink() {
        Chat chat = new Chat(100L, java.time.Instant.now());
        chatRepository.save(chat);

        Link link = new Link("https://github.com/test", java.util.Set.of(), java.time.Instant.now(), null);
        linkRepository.save(100L, link);

        var found = linkRepository.findByChatIdAndUrl(100L, "https://github.com/test");
        assertThat(found).isPresent();
        assertThat(found.get().url()).isEqualTo("https://github.com/test");
    }

    @Test
    void shouldFindAllLinksByChat() {
        Chat chat = new Chat(200L, java.time.Instant.now());
        chatRepository.save(chat);

        Link link1 = new Link("https://github.com/test1", java.util.Set.of(), java.time.Instant.now(), null);
        Link link2 = new Link("https://github.com/test2", java.util.Set.of(), java.time.Instant.now(), null);

        linkRepository.save(200L, link1);
        linkRepository.save(200L, link2);

        var links = linkRepository.findAllByChatId(200L);
        assertThat(links).hasSize(2);
    }

    @Test
    void shouldDeleteLink() {
        Chat chat = new Chat(300L, java.time.Instant.now());
        chatRepository.save(chat);

        Link link = new Link("https://github.com/delete", java.util.Set.of(), java.time.Instant.now(), null);
        linkRepository.save(300L, link);

        linkRepository.deleteByChatIdAndUrl(300L, "https://github.com/delete");

        var found = linkRepository.findByChatIdAndUrl(300L, "https://github.com/delete");
        assertThat(found).isEmpty();
    }

    @Test
    void shouldUpdateLink() {
        Chat chat = new Chat(400L, java.time.Instant.now());
        chatRepository.save(chat);

        Link link = new Link("https://github.com/update", java.util.Set.of(), java.time.Instant.now(), null);
        linkRepository.save(400L, link);

        java.time.Instant newTime = java.time.Instant.now().plusSeconds(3600);
        Link updated = new Link("https://github.com/update", java.util.Set.of(), newTime, newTime);
        linkRepository.updateLink(updated);

        var found = linkRepository.findByChatIdAndUrl(400L, "https://github.com/update");
        assertThat(found).isPresent();
        assertThat(found.get().lastUpdatedAt()).isNotNull();
    }

    @Test
    void shouldSaveLinkWithTags() {
        Chat chat = new Chat(500L, java.time.Instant.now());
        chatRepository.save(chat);

        Link link = new Link("https://github.com/tagged", java.util.Set.of("java", "spring"), java.time.Instant.now(), null);
        linkRepository.save(500L, link);

        var found = linkRepository.findByChatIdAndUrl(500L, "https://github.com/tagged");
        assertThat(found).isPresent();
        assertThat(found.get().tags()).containsExactlyInAnyOrder("java", "spring");
    }

    @Test
    void shouldFindLinksByTag() {
        Chat chat = new Chat(600L, java.time.Instant.now());
        chatRepository.save(chat);

        Link link1 = new Link("https://github.com/java1", java.util.Set.of("java"), java.time.Instant.now(), null);
        Link link2 = new Link("https://github.com/java2", java.util.Set.of("java"), java.time.Instant.now(), null);
        Link link3 = new Link("https://github.com/python", java.util.Set.of("python"), java.time.Instant.now(), null);

        linkRepository.save(600L, link1);
        linkRepository.save(600L, link2);
        linkRepository.save(600L, link3);

        var javaLinks = linkRepository.findAllByChatIdAndTag(600L, "java");
        assertThat(javaLinks).hasSize(2);
    }

    @Test
    void shouldFindChatIdsByUrl() {
        Chat chat1 = new Chat(700L, java.time.Instant.now());
        Chat chat2 = new Chat(701L, java.time.Instant.now());
        chatRepository.save(chat1);
        chatRepository.save(chat2);

        Link link = new Link("https://github.com/shared", java.util.Set.of(), java.time.Instant.now(), null);
        linkRepository.save(700L, link);
        linkRepository.save(701L, link);

        var chatIds = linkRepository.findChatIdsByUrl("https://github.com/shared");
        assertThat(chatIds).containsExactlyInAnyOrder(700L, 701L);
    }

    @Test
    void shouldManageTags() {
        tagRepository.save("java");
        tagRepository.save("spring");

        assertThat(tagRepository.existsByName("java")).isTrue();
        assertThat(tagRepository.findByName("java")).isPresent();

        var allTags = tagRepository.findAll();
        assertThat(allTags).containsExactlyInAnyOrder("java", "spring");

        tagRepository.deleteByName("java");
        assertThat(tagRepository.existsByName("java")).isFalse();
    }
}

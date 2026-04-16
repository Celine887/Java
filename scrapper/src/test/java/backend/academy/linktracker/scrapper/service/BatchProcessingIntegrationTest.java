package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.BaseIntegrationTest;
import backend.academy.linktracker.scrapper.model.Chat;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "jpa"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BatchProcessingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private ChatRepository chatRepository;

    private Long testChatId;

    @BeforeEach
    void setUp() {
        testChatId = 999000L + (long) (Math.random() * 1000);
        chatRepository.save(new Chat(testChatId, Instant.now()));
    }

    @Test
    void shouldLoadLinksInBatches() {
        for (int i = 0; i < 150; i++) {
            Link link = new Link(
                    "https://github.com/batch-test/repo" + i + "-" + testChatId,
                    Set.of(),
                    Instant.now().minusSeconds(150 - i),
                    null);
            linkRepository.save(testChatId, link);
        }

        var allLinks = linkRepository.findLinksForUpdate(1000, 0);
        var testLinks = allLinks.stream()
                .filter(l -> l.url().contains("batch-test") && l.url().contains(String.valueOf(testChatId)))
                .toList();

        assertThat(testLinks).hasSize(150);

        var batch1 = linkRepository.findLinksForUpdate(100, 0);
        var testBatch1 = batch1.stream()
                .filter(l -> l.url().contains("batch-test") && l.url().contains(String.valueOf(testChatId)))
                .toList();
        assertThat(testBatch1.size()).isGreaterThanOrEqualTo(50);

        var batch2 = linkRepository.findLinksForUpdate(100, 100);
        var testBatch2 = batch2.stream()
                .filter(l -> l.url().contains("batch-test") && l.url().contains(String.valueOf(testChatId)))
                .toList();
        assertThat(testBatch2.size()).isGreaterThanOrEqualTo(30);
    }

    @Test
    void shouldOrderLinksByLastCheckedAt() {
        Long uniqueChatId = 998000L + (long) (Math.random() * 1000);
        chatRepository.save(new Chat(uniqueChatId, Instant.now()));

        Instant oldTime = Instant.now().minusSeconds(10000);
        Instant newTime = Instant.now().minusSeconds(5000);

        Link oldLink = new Link("https://github.com/order-test/old-" + uniqueChatId, Set.of(), oldTime, null);
        Link newLink = new Link("https://github.com/order-test/new-" + uniqueChatId, Set.of(), newTime, null);

        linkRepository.save(uniqueChatId, newLink);
        linkRepository.save(uniqueChatId, oldLink);

        var batch = linkRepository.findLinksForUpdate(1000, 0);
        var testLinks = batch.stream()
                .filter(l -> l.url().contains("order-test") && l.url().contains(String.valueOf(uniqueChatId)))
                .toList();

        assertThat(testLinks).hasSize(2);
        assertThat(testLinks.get(0).url()).contains("old");
        assertThat(testLinks.get(1).url()).contains("new");
    }
}

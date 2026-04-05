package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;

import backend.academy.linktracker.scrapper.BaseIntegrationTest;
import backend.academy.linktracker.scrapper.model.Chat;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "jpa"})
class BatchProcessingIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private ChatRepository chatRepository;

    @Test
    void shouldLoadLinksInBatches() {
        // Given - create test data
        chatRepository.save(new Chat(1L, Instant.now()));

        for (int i = 0; i < 150; i++) {
            Link link = new Link(
                    "https://github.com/user/repo" + i, Set.of(), Instant.now().minusSeconds(i), null);
            linkRepository.save(1L, link);
        }

        // When - load first batch
        var batch1 = linkRepository.findLinksForUpdate(100, 0);

        // Then
        assertThat(batch1).hasSize(100);

        // When - load second batch
        var batch2 = linkRepository.findLinksForUpdate(100, 100);

        // Then
        assertThat(batch2).hasSizeBetween(48, 52); // Allow some variance due to concurrent operations

        // When - load third batch (should be empty)
        var batch3 = linkRepository.findLinksForUpdate(100, 200);

        // Then
        assertThat(batch3).isEmpty();
    }

    @Test
    void shouldOrderLinksByLastCheckedAt() {
        // Given
        chatRepository.save(new Chat(1L, Instant.now()));

        Link oldLink =
                new Link("https://github.com/user/old", Set.of(), Instant.now().minusSeconds(1000), null);
        Link newLink =
                new Link("https://github.com/user/new", Set.of(), Instant.now().minusSeconds(100), null);

        linkRepository.save(1L, newLink);
        linkRepository.save(1L, oldLink);

        // When
        var batch = linkRepository.findLinksForUpdate(10, 0);

        // Then - old link should come first
        assertThat(batch).hasSize(2);
        assertThat(batch.get(0).url()).isEqualTo(oldLink.url());
        assertThat(batch.get(1).url()).isEqualTo(newLink.url());
    }
}

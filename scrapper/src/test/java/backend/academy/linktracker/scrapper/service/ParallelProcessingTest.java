package backend.academy.linktracker.scrapper.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import backend.academy.linktracker.scrapper.client.UpdateClient;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.checker.LinkChecker;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParallelProcessingTest {

    @Mock
    private LinkChecker linkChecker;

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private UpdateClient updateClient;

    private LinkUpdateService linkUpdateService;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() {
        executorService = Executors.newFixedThreadPool(4);
        linkUpdateService = new LinkUpdateService(
                List.of(linkChecker),
                linkRepository,
                updateClient,
                executorService,
                1000, // batch-size
                250); // sub-batch-size
    }

    @Test
    void shouldProcessLinksInParallel() {
        // Given - 1000 links
        List<Link> links = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            links.add(new Link("https://github.com/user/repo" + i, Set.of(), Instant.now(), null));
        }

        // Track which threads processed links
        Set<String> threadNames = ConcurrentHashMap.newKeySet();
        AtomicInteger processedCount = new AtomicInteger(0);

        when(linkRepository.findLinksForUpdate(1000, 0)).thenReturn(links);
        when(linkRepository.findLinksForUpdate(1000, 1000)).thenReturn(List.of());
        when(linkChecker.supports(anyString())).thenReturn(true);
        when(linkChecker.checkForUpdates(anyString(), any(Instant.class))).thenAnswer(invocation -> {
            threadNames.add(Thread.currentThread().getName());
            processedCount.incrementAndGet();
            // Simulate some work
            Thread.sleep(1);
            return List.of();
        });

        // When
        linkUpdateService.updateAllLinks();

        // Then - should use multiple threads
        assertThat(threadNames.size()).isGreaterThan(1);
        assertThat(processedCount.get()).isEqualTo(1000);
    }

    @Test
    void shouldSplitBatchIntoSubBatches() {
        // Given - 1000 links should be split into 4 sub-batches of 250
        List<Link> links = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            links.add(new Link("https://github.com/user/repo" + i, Set.of(), Instant.now(), null));
        }

        AtomicInteger subBatchCount = new AtomicInteger(0);
        Set<Integer> subBatchSizes = ConcurrentHashMap.newKeySet();

        when(linkRepository.findLinksForUpdate(1000, 0)).thenReturn(links);
        when(linkRepository.findLinksForUpdate(1000, 1000)).thenReturn(List.of());
        when(linkChecker.supports(anyString())).thenReturn(true);
        when(linkChecker.checkForUpdates(anyString(), any(Instant.class))).thenAnswer(invocation -> {
            int count = subBatchCount.incrementAndGet();
            // Every 250 links is a new sub-batch
            if (count % 250 == 1) {
                subBatchSizes.add(count);
            }
            return List.of();
        });

        // When
        linkUpdateService.updateAllLinks();

        // Then - should have 4 sub-batches
        assertThat(subBatchSizes).hasSize(4);
    }

    @Test
    void shouldIsolateErrorsInParallelProcessing() {
        // Given - some links will fail
        List<Link> links = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            links.add(new Link("https://github.com/user/repo" + i, Set.of(), Instant.now(), null));
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        when(linkRepository.findLinksForUpdate(1000, 0)).thenReturn(links);
        when(linkRepository.findLinksForUpdate(1000, 1000)).thenReturn(List.of());
        when(linkChecker.supports(anyString())).thenReturn(true);
        when(linkChecker.checkForUpdates(anyString(), any(Instant.class))).thenAnswer(invocation -> {
            String url = invocation.getArgument(0);
            // Every 10th link fails
            if (url.endsWith("0") || url.endsWith("5")) {
                failureCount.incrementAndGet();
                throw new RuntimeException("Simulated API error");
            }
            successCount.incrementAndGet();
            return List.of();
        });

        // When
        linkUpdateService.updateAllLinks();

        // Then - failures should not affect successful processing
        assertThat(successCount.get()).isEqualTo(80);
        assertThat(failureCount.get()).isEqualTo(20);
    }
}

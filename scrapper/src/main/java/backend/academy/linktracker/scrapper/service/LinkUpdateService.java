package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.UpdateClient;
import backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.model.LinkProcessingResult;
import backend.academy.linktracker.scrapper.model.LinkUpdateInfo;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.checker.LinkChecker;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for checking and processing link updates.
 * Implements batch processing with parallel execution for improved performance.
 *
 * <p>Processing flow:
 * <ol>
 *   <li>Load batch of links (e.g., 1000 links)</li>
 *   <li>Split batch into sub-batches (e.g., 4 x 250 links)</li>
 *   <li>Process each sub-batch in parallel using thread pool</li>
 *   <li>Collect results and handle errors</li>
 *   <li>Repeat for next batch</li>
 * </ol>
 *
 * <p>Error handling: Errors in processing individual links are isolated
 * and do not affect other links in the batch.
 */
@Service
public class LinkUpdateService {
    private static final Logger log = LoggerFactory.getLogger(LinkUpdateService.class);

    private final List<LinkChecker> checkers;
    private final LinkRepository linkRepository;
    private final UpdateClient updateClient;
    private final ExecutorService executorService;
    private final int batchSize;
    private final int subBatchSize;

    public LinkUpdateService(
            List<LinkChecker> checkers,
            LinkRepository linkRepository,
            UpdateClient updateClient,
            ExecutorService executorService,
            @Value("${app.scheduler.batch-size:1000}") int batchSize,
            @Value("${app.scheduler.sub-batch-size:250}") int subBatchSize) {
        this.checkers = checkers;
        this.linkRepository = linkRepository;
        this.updateClient = updateClient;
        this.executorService = executorService;
        this.batchSize = batchSize;
        this.subBatchSize = subBatchSize;
    }

    public void updateAllLinks() {
        log.info("Starting link update check with batch-size={}, sub-batch-size={}", batchSize, subBatchSize);
        int offset = 0;
        int totalProcessed = 0;
        int totalFailed = 0;

        while (true) {
            List<Link> batch = linkRepository.findLinksForUpdate(batchSize, offset);
            if (batch.isEmpty()) {
                break;
            }

            log.debug("Processing batch: offset={}, size={}", offset, batch.size());
            List<LinkProcessingResult> results = processBatchParallel(batch);

            long failed = results.stream().filter(r -> !r.success()).count();
            totalProcessed += results.size();
            totalFailed += (int) failed;

            if (failed > 0) {
                notifyFailedLinks(results);
            }

            offset += batchSize;
        }

        log.info("Link update check completed: total={}, failed={}", totalProcessed, totalFailed);
    }

    private List<LinkProcessingResult> processBatchParallel(List<Link> batch) {
        List<LinkProcessingResult> allResults = new ArrayList<>();
        List<Callable<List<LinkProcessingResult>>> tasks = new ArrayList<>();

        // Split batch into sub-batches for parallel processing
        for (int i = 0; i < batch.size(); i += subBatchSize) {
            int end = Math.min(i + subBatchSize, batch.size());
            List<Link> subBatch = batch.subList(i, end);
            tasks.add(() -> processSubBatch(subBatch));
        }

        try {
            List<Future<List<LinkProcessingResult>>> futures = executorService.invokeAll(tasks);
            for (Future<List<LinkProcessingResult>> future : futures) {
                allResults.addAll(future.get());
            }
        } catch (Exception e) {
            log.error("Error during parallel batch processing", e);
        }

        return allResults;
    }

    private List<LinkProcessingResult> processSubBatch(List<Link> subBatch) {
        List<LinkProcessingResult> results = new ArrayList<>();
        for (Link link : subBatch) {
            try {
                updateLink(link);
                results.add(LinkProcessingResult.success(link.url()));
            } catch (Exception e) {
                log.error("Error checking link: url={}", link.url(), e);
                results.add(LinkProcessingResult.failure(link.url(), e.getMessage()));
            }
        }
        return results;
    }

    private void updateLink(Link link) {
        LinkChecker checker = checkers.stream()
                .filter(c -> c.supports(link.url()))
                .findFirst()
                .orElse(null);

        if (checker == null) {
            log.warn("No checker found for URL: {}", link.url());
            return;
        }

        Instant lastChecked = link.lastCheckedAt() != null ? link.lastCheckedAt() : Instant.EPOCH;
        List<LinkUpdateInfo> updates = checker.checkForUpdates(link.url(), lastChecked);

        if (!updates.isEmpty()) {
            log.info("Found {} updates for link: {}", updates.size(), link.url());
            for (LinkUpdateInfo update : updates) {
                notifyUpdate(link, update);
            }
            linkRepository.updateLink(link.withLastUpdatedAt(Instant.now()).withLastCheckedAt(Instant.now()));
        } else {
            linkRepository.updateLink(link.withLastCheckedAt(Instant.now()));
        }
    }

    private void notifyUpdate(Link link, LinkUpdateInfo updateInfo) {
        List<Long> chatIds = linkRepository.findChatIdsByUrl(link.url());
        if (chatIds.isEmpty()) {
            log.warn("No chats tracking link: {}", link.url());
            return;
        }

        String description = updateInfo.formatMessage();
        LinkUpdateRequest request =
                new LinkUpdateRequest(generateId(updateInfo.url()), updateInfo.url(), description, chatIds);

        try {
            updateClient.sendUpdate(request);
        } catch (Exception e) {
            log.error("Failed to send update notification: url={}", updateInfo.url(), e);
        }
    }

    private void notifyFailedLinks(List<LinkProcessingResult> results) {
        List<String> failedUrls = results.stream()
                .filter(r -> !r.success())
                .map(LinkProcessingResult::url)
                .toList();

        log.warn("Failed to process {} links: {}", failedUrls.size(), failedUrls);
    }

    private Long generateId(String url) {
        return (long) url.hashCode();
    }
}

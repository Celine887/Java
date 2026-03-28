package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.UpdateClient;
import backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.checker.LinkChecker;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LinkUpdateService {
    private static final Logger log = LoggerFactory.getLogger(LinkUpdateService.class);

    private final List<LinkChecker> checkers;
    private final LinkRepository linkRepository;
    private final UpdateClient updateClient;

    public LinkUpdateService(List<LinkChecker> checkers, LinkRepository linkRepository, UpdateClient updateClient) {
        this.checkers = checkers;
        this.linkRepository = linkRepository;
        this.updateClient = updateClient;
    }

    public void updateAllLinks() {
        log.debug("Starting link update check");
        List<Link> links = linkRepository.findAllLinks();

        for (Link link : links) {
            try {
                updateLink(link);
            } catch (Exception e) {
                log.error("Error checking link: url={}", link.url(), e);
            }
        }
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

        Optional<Instant> lastUpdate = checker.getLastUpdate(link.url());
        if (lastUpdate.isEmpty()) {
            log.warn("Failed to fetch update for URL: {}", link.url());
            return;
        }

        Instant remoteUpdated = lastUpdate.orElseThrow();
        if (link.lastUpdatedAt() == null || remoteUpdated.isAfter(link.lastUpdatedAt())) {
            log.info("Link updated: url={}", link.url());
            notifyUpdate(link, "Link updated");
            linkRepository.updateLink(link.withLastUpdatedAt(remoteUpdated).withLastCheckedAt(Instant.now()));
        } else {
            linkRepository.updateLink(link.withLastCheckedAt(Instant.now()));
        }
    }

    private void notifyUpdate(Link link, String description) {
        List<Long> chatIds = linkRepository.findChatIdsByUrl(link.url());
        if (chatIds.isEmpty()) {
            log.warn("No chats tracking link: {}", link.url());
            return;
        }

        LinkUpdateRequest request = new LinkUpdateRequest(generateId(link.url()), link.url(), description, chatIds);
        updateClient.sendUpdate(request);
    }

    private Long generateId(String url) {
        return (long) url.hashCode();
    }
}

package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.client.BotClient;
import backend.academy.linktracker.scrapper.client.dto.LinkUpdateRequest;
import backend.academy.linktracker.scrapper.model.Link;
import backend.academy.linktracker.scrapper.repository.LinkRepository;
import backend.academy.linktracker.scrapper.service.checker.LinkChecker;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkUpdateService {

    private final List<LinkChecker> checkers;
    private final LinkRepository linkRepository;
    private final BotClient botClient;

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
        botClient.sendUpdate(request);
    }

    private Long generateId(String url) {
        return (long) url.hashCode();
    }
}

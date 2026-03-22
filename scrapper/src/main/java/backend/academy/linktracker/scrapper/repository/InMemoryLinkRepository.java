package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.Link;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "memory", matchIfMissing = true)
public class InMemoryLinkRepository implements LinkRepository {
    private final Map<Long, Map<String, Link>> chatLinks = new ConcurrentHashMap<>();

    @Override
    public void save(Long chatId, Link link) {
        chatLinks.computeIfAbsent(chatId, k -> new ConcurrentHashMap<>()).put(link.url(), link);
    }

    @Override
    public Optional<Link> findByChatIdAndUrl(Long chatId, String url) {
        return Optional.ofNullable(chatLinks.get(chatId)).map(links -> links.get(url));
    }

    @Override
    public List<Link> findAllByChatId(Long chatId) {
        return Optional.ofNullable(chatLinks.get(chatId))
                .map(links -> List.copyOf(links.values()))
                .orElse(List.of());
    }

    @Override
    public List<Link> findAllByChatIdAndTag(Long chatId, String tag) {
        return findAllByChatId(chatId).stream()
                .filter(link -> link.tags() != null && link.tags().contains(tag))
                .toList();
    }

    @Override
    public void deleteByChatIdAndUrl(Long chatId, String url) {
        Optional.ofNullable(chatLinks.get(chatId)).ifPresent(links -> links.remove(url));
    }

    @Override
    public List<Link> findAllLinks() {
        return chatLinks.values().stream()
                .flatMap(links -> links.values().stream())
                .distinct()
                .toList();
    }

    @Override
    public void updateLink(Link link) {
        chatLinks.values().forEach(links -> {
            if (links.containsKey(link.url())) {
                links.put(link.url(), link);
            }
        });
    }

    @Override
    public List<Long> findChatIdsByUrl(String url) {
        return chatLinks.entrySet().stream()
                .filter(entry -> entry.getValue().containsKey(url))
                .map(Map.Entry::getKey)
                .toList();
    }
}

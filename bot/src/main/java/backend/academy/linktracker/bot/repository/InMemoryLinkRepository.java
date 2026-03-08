package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.TrackedLink;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryLinkRepository implements LinkRepository {
    private final Map<Long, Map<String, TrackedLink>> chatLinks = new ConcurrentHashMap<>();

    @Override
    public void save(Long chatId, TrackedLink link) {
        chatLinks.computeIfAbsent(chatId, k -> new ConcurrentHashMap<>()).put(link.url(), link);
    }

    @Override
    public Optional<TrackedLink> findByChatIdAndUrl(Long chatId, String url) {
        return Optional.ofNullable(chatLinks.get(chatId)).map(links -> links.get(url));
    }

    @Override
    public List<TrackedLink> findAllByChatId(Long chatId) {
        return Optional.ofNullable(chatLinks.get(chatId))
                .map(links -> List.copyOf(links.values()))
                .orElse(List.of());
    }

    @Override
    public List<TrackedLink> findAllByChatIdAndTag(Long chatId, String tag) {
        return findAllByChatId(chatId).stream()
                .filter(link -> link.tags() != null && link.tags().contains(tag))
                .toList();
    }

    @Override
    public void deleteByChatIdAndUrl(Long chatId, String url) {
        Optional.ofNullable(chatLinks.get(chatId)).ifPresent(links -> links.remove(url));
    }

    @Override
    public boolean existsByChatIdAndUrl(Long chatId, String url) {
        return findByChatIdAndUrl(chatId, url).isPresent();
    }
}

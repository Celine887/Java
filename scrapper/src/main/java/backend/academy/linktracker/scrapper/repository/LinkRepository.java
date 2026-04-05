package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.Link;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface LinkRepository {
    void save(Long chatId, Link link);

    Optional<Link> findByChatIdAndUrl(Long chatId, String url);

    List<Link> findAllByChatId(Long chatId);

    List<Link> findAllByChatIdAndTag(Long chatId, String tag);

    void deleteByChatIdAndUrl(Long chatId, String url);

    List<Link> findAllLinks();

    List<Link> findLinksForUpdate(int limit, Instant olderThan);

    List<Link> findLinksForUpdate(int limit, int offset);

    void updateLink(Link link);

    List<Long> findChatIdsByUrl(String url);
}

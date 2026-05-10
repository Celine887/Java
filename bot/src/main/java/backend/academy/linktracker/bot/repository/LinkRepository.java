package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.TrackedLink;
import java.util.List;
import java.util.Optional;

public interface LinkRepository {
    void save(Long chatId, TrackedLink link);

    Optional<TrackedLink> findByChatIdAndUrl(Long chatId, String url);

    List<TrackedLink> findAllByChatId(Long chatId);

    List<TrackedLink> findAllByChatIdAndTag(Long chatId, String tag);

    void deleteByChatIdAndUrl(Long chatId, String url);

    boolean existsByChatIdAndUrl(Long chatId, String url);
}

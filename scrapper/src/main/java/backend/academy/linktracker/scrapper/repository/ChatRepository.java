package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.Chat;
import java.util.Optional;

public interface ChatRepository {
    void save(Chat chat);

    Optional<Chat> findById(Long id);

    boolean existsById(Long id);

    void deleteById(Long id);
}

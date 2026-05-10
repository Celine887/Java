package backend.academy.linktracker.scrapper.repository;

import backend.academy.linktracker.scrapper.model.Chat;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.database.access-type", havingValue = "memory", matchIfMissing = true)
public class InMemoryChatRepository implements ChatRepository {
    private final Map<Long, Chat> chats = new ConcurrentHashMap<>();

    @Override
    public void save(Chat chat) {
        chats.put(chat.id(), chat);
    }

    @Override
    public Optional<Chat> findById(Long id) {
        return Optional.ofNullable(chats.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return chats.containsKey(id);
    }

    @Override
    public void deleteById(Long id) {
        chats.remove(id);
    }
}

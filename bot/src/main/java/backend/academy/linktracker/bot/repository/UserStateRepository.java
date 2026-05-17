package backend.academy.linktracker.bot.repository;

import backend.academy.linktracker.bot.model.UserContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class UserStateRepository {
    private final Map<Long, UserContext> userStates = new ConcurrentHashMap<>();

    public UserContext getContext(Long chatId) {
        return userStates.computeIfAbsent(
                chatId, k -> new UserContext(backend.academy.linktracker.bot.model.UserState.IDLE, null));
    }

    public void saveContext(Long chatId, UserContext context) {
        userStates.put(chatId, context);
    }

    public void clearContext(Long chatId) {
        userStates.put(chatId, new UserContext(backend.academy.linktracker.bot.model.UserState.IDLE, null));
    }
}

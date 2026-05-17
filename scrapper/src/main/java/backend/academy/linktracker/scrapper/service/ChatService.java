package backend.academy.linktracker.scrapper.service;

import backend.academy.linktracker.scrapper.model.Chat;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;

    public void register(Long chatId) {
        if (chatRepository.existsById(chatId)) {
            throw new IllegalArgumentException("Chat already registered");
        }
        Chat chat = new Chat(chatId, Instant.now());
        chatRepository.save(chat);
    }

    public void unregister(Long chatId) {
        if (!chatRepository.existsById(chatId)) {
            throw new IllegalArgumentException("Chat not found");
        }
        chatRepository.deleteById(chatId);
    }

    public boolean exists(Long chatId) {
        return chatRepository.existsById(chatId);
    }
}

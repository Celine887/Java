package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.entity.ChatEntity;
import backend.academy.linktracker.scrapper.model.Chat;
import backend.academy.linktracker.scrapper.repository.ChatRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class JpaChatRepositoryImpl implements ChatRepository {
    private final JpaChatEntityRepository entityRepository;

    @Override
    @Transactional
    public void save(Chat chat) {
        ChatEntity entity = new ChatEntity(chat.id());
        entityRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Chat> findById(Long id) {
        return entityRepository.findById(id)
            .map(entity -> new Chat(entity.getId(), entity.getCreatedAt().toInstant()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return entityRepository.existsById(id);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        entityRepository.deleteById(id);
    }
}

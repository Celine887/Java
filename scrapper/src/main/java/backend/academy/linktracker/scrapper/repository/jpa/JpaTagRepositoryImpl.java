package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.entity.TagEntity;
import backend.academy.linktracker.scrapper.repository.TagRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
public class JpaTagRepositoryImpl implements TagRepository {
    private final JpaTagEntityRepository entityRepository;

    @Override
    @Transactional
    public void save(String tagName) {
        if (!entityRepository.findByName(tagName).isPresent()) {
            entityRepository.save(new TagEntity(tagName));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findById(Long id) {
        return entityRepository.findById(id).map(TagEntity::getName);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findByName(String name) {
        return entityRepository.findByName(name).map(TagEntity::getName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAll() {
        return entityRepository.findAll().stream()
            .map(TagEntity::getName)
            .toList();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        entityRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByName(String name) {
        entityRepository.findByName(name).ifPresent(entityRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByName(String name) {
        return entityRepository.findByName(name).isPresent();
    }
}

package backend.academy.linktracker.scrapper.repository;

import java.util.List;
import java.util.Optional;

public interface TagRepository {
    void save(String tagName);

    Optional<String> findById(Long id);

    Optional<String> findByName(String name);

    List<String> findAll();

    void deleteById(Long id);

    void deleteByName(String name);

    boolean existsByName(String name);
}

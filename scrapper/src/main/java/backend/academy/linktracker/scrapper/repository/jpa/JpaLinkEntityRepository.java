package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.entity.LinkEntity;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaLinkEntityRepository extends JpaRepository<LinkEntity, Long> {
    Optional<LinkEntity> findByUrl(String url);

    @Query("SELECT l FROM LinkEntity l JOIN l.chats c WHERE c.id = :chatId")
    List<LinkEntity> findAllByChatId(@Param("chatId") Long chatId, Pageable pageable);

    @Query("SELECT l FROM LinkEntity l JOIN l.chats c JOIN l.tags t WHERE c.id = :chatId AND t.name = :tagName")
    List<LinkEntity> findAllByChatIdAndTagName(
            @Param("chatId") Long chatId, @Param("tagName") String tagName, Pageable pageable);

    @Query("SELECT l FROM LinkEntity l WHERE l.lastCheckedAt < :threshold")
    List<LinkEntity> findLinksToCheck(@Param("threshold") OffsetDateTime threshold, Pageable pageable);

    @Query(
            "SELECT l FROM LinkEntity l WHERE l.lastCheckedAt < :threshold OR l.lastCheckedAt IS NULL ORDER BY l.lastCheckedAt ASC NULLS FIRST")
    List<LinkEntity> findLinksForUpdate(@Param("threshold") OffsetDateTime threshold, Pageable pageable);

    @Query("SELECT l FROM LinkEntity l ORDER BY l.lastCheckedAt ASC NULLS FIRST")
    List<LinkEntity> findAllOrderByLastCheckedAt(Pageable pageable);

    @Query("SELECT c.id FROM LinkEntity l JOIN l.chats c WHERE l.url = :url")
    List<Long> findChatIdsByUrl(@Param("url") String url);
}

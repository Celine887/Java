package backend.academy.linktracker.scrapper.repository.jpa;

import backend.academy.linktracker.scrapper.entity.OutboxEventEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaOutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    @Query("SELECT e FROM OutboxEventEntity e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<OutboxEventEntity> findPendingEvents();

    @Modifying
    @Query("UPDATE OutboxEventEntity e SET e.status = :status WHERE e.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") OutboxEventEntity.OutboxStatus status);
}

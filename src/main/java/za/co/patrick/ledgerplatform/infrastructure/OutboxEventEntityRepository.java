package za.co.patrick.ledgerplatform.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventEntityRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findAllByOrderByCreatedAtDesc();

    List<OutboxEventEntity> findAllByPublishedAtIsNullOrderByCreatedAtAsc();

    List<OutboxEventEntity> findAllByPublishedAtIsNotNullOrderByPublishedAtDescCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select event
            from OutboxEventEntity event
            where event.publishedAt is null
            order by event.createdAt asc
            """)
    Page<OutboxEventEntity> findUnpublishedForUpdate(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select event
            from OutboxEventEntity event
            where event.id = :eventId
            """)
    Optional<OutboxEventEntity> findByIdForUpdate(UUID eventId);
}

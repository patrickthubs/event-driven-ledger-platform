package za.co.patrick.ledgerplatform.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventEntityRepository extends JpaRepository<OutboxEventEntity, UUID> {

    List<OutboxEventEntity> findAllByOrderByCreatedAtDesc();

    List<OutboxEventEntity> findAllByPublishedAtIsNullOrderByCreatedAtAsc();

    List<OutboxEventEntity> findAllByPublishedAtIsNotNullOrderByPublishedAtDescCreatedAtDesc();
}

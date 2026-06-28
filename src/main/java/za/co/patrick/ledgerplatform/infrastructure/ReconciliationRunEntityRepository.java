package za.co.patrick.ledgerplatform.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.patrick.ledgerplatform.domain.ReconciliationStatus;

import java.util.List;
import java.util.UUID;

public interface ReconciliationRunEntityRepository extends JpaRepository<ReconciliationRunEntity, UUID> {

    List<ReconciliationRunEntity> findAllByAccount_Id(UUID accountId);

    List<ReconciliationRunEntity> findAllByStatus(ReconciliationStatus status);

    List<ReconciliationRunEntity> findAllByAccount_IdAndStatus(UUID accountId, ReconciliationStatus status);
}

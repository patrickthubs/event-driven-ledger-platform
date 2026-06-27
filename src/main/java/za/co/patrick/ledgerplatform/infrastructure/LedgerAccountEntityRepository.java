package za.co.patrick.ledgerplatform.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerAccountEntityRepository extends JpaRepository<LedgerAccountEntity, UUID> {

    Optional<LedgerAccountEntity> findByAccountNumber(String accountNumber);
}

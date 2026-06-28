package za.co.patrick.ledgerplatform.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserEntityRepository extends JpaRepository<AppUserEntity, UUID> {

    Optional<AppUserEntity> findByUsernameIgnoreCase(String username);
}

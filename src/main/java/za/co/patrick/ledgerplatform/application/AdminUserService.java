package za.co.patrick.ledgerplatform.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.patrick.ledgerplatform.api.AdminUserResponse;
import za.co.patrick.ledgerplatform.infrastructure.AppUserEntityRepository;

import java.util.List;

@Service
public class AdminUserService {

    private final AppUserEntityRepository appUserRepository;

    public AdminUserService(AppUserEntityRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return appUserRepository.findAll().stream()
                .sorted((left, right) -> left.getUsername().compareToIgnoreCase(right.getUsername()))
                .map(user -> new AdminUserResponse(
                        user.getUsername(),
                        user.isEnabled(),
                        user.getRoles(),
                        user.getCreatedAt(),
                        user.getUpdatedAt()
                ))
                .toList();
    }
}

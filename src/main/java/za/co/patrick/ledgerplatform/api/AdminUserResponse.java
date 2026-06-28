package za.co.patrick.ledgerplatform.api;

import za.co.patrick.ledgerplatform.domain.AppRole;

import java.time.OffsetDateTime;
import java.util.Set;

public record AdminUserResponse(
        String username,
        boolean enabled,
        Set<AppRole> roles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

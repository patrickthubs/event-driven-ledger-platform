package za.co.patrick.ledgerplatform.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.patrick.ledgerplatform.api.AssignReconciliationRequest;
import za.co.patrick.ledgerplatform.api.CreateReconciliationRequest;
import za.co.patrick.ledgerplatform.api.ReconciliationResponse;
import za.co.patrick.ledgerplatform.api.ResolveReconciliationRequest;
import za.co.patrick.ledgerplatform.domain.ReconciliationStatus;
import za.co.patrick.ledgerplatform.infrastructure.LedgerAccountEntity;
import za.co.patrick.ledgerplatform.infrastructure.LedgerAccountEntityRepository;
import za.co.patrick.ledgerplatform.infrastructure.ReconciliationRunEntity;
import za.co.patrick.ledgerplatform.infrastructure.ReconciliationRunEntityRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class ReconciliationService {

    private final ReconciliationRunEntityRepository reconciliationRunRepository;
    private final LedgerAccountEntityRepository ledgerAccountRepository;
    private final LedgerService ledgerService;

    public ReconciliationService(
            ReconciliationRunEntityRepository reconciliationRunRepository,
            LedgerAccountEntityRepository ledgerAccountRepository,
            LedgerService ledgerService
    ) {
        this.reconciliationRunRepository = reconciliationRunRepository;
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional
    public ReconciliationResponse createReconciliation(CreateReconciliationRequest request) {
        if (request.from().isAfter(request.to())) {
            throw new IllegalArgumentException("Reconciliation start date must be before or equal to the end date.");
        }

        LedgerAccountEntity account = ledgerAccountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account %s was not found.".formatted(request.accountId())));

        BigDecimal ledgerBalance = ledgerService.getAccountStatement(request.accountId(), request.from(), request.to()).closingBalance();
        BigDecimal externalBalance = money(request.externalBalance());
        BigDecimal toleranceAmount = money(request.toleranceAmount() == null ? BigDecimal.ZERO : request.toleranceAmount());
        if (toleranceAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Reconciliation tolerance amount cannot be negative.");
        }
        BigDecimal differenceAmount = money(ledgerBalance.subtract(externalBalance));
        ReconciliationStatus status = differenceAmount.abs().compareTo(toleranceAmount) <= 0
                ? ReconciliationStatus.MATCHED
                : ReconciliationStatus.MISMATCHED;
        OffsetDateTime now = OffsetDateTime.now();

        ReconciliationRunEntity entity = reconciliationRunRepository.save(new ReconciliationRunEntity(
                UUID.randomUUID(),
                account,
                account.getCurrency(),
                request.from(),
                request.to(),
                ledgerBalance,
                externalBalance,
                toleranceAmount,
                differenceAmount,
                status,
                trimToNull(request.externalReference()),
                trimToNull(request.notes()),
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                status == ReconciliationStatus.MATCHED ? now : null
        ));

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationResponse> listReconciliations(UUID accountId, ReconciliationStatus status) {
        List<ReconciliationRunEntity> runs;
        if (accountId != null && status != null) {
            runs = reconciliationRunRepository.findAllByAccount_IdAndStatus(accountId, status);
        } else if (accountId != null) {
            runs = reconciliationRunRepository.findAllByAccount_Id(accountId);
        } else if (status != null) {
            runs = reconciliationRunRepository.findAllByStatus(status);
        } else {
            runs = reconciliationRunRepository.findAll();
        }
        return runs.stream()
                .sorted(Comparator.comparing(ReconciliationRunEntity::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReconciliationResponse getReconciliation(UUID reconciliationId) {
        return reconciliationRunRepository.findById(reconciliationId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation %s was not found.".formatted(reconciliationId)));
    }

    @Transactional
    public ReconciliationResponse assignForReview(UUID reconciliationId, AssignReconciliationRequest request) {
        ReconciliationRunEntity entity = reconciliationRunRepository.findById(reconciliationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation %s was not found.".formatted(reconciliationId)));
        if (entity.getStatus() == ReconciliationStatus.MATCHED || entity.getStatus() == ReconciliationStatus.RESOLVED) {
            throw new IllegalArgumentException("Only mismatched reconciliation runs can move into review.");
        }
        entity.moveToReview(trimToNull(request.assignedTo()), trimToNull(request.reviewNotes()));
        return toResponse(reconciliationRunRepository.save(entity));
    }

    @Transactional
    public ReconciliationResponse resolve(UUID reconciliationId, ResolveReconciliationRequest request) {
        ReconciliationRunEntity entity = reconciliationRunRepository.findById(reconciliationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reconciliation %s was not found.".formatted(reconciliationId)));
        if (entity.getStatus() == ReconciliationStatus.MATCHED) {
            throw new IllegalArgumentException("Matched reconciliation runs do not need resolution.");
        }
        if (entity.getStatus() == ReconciliationStatus.RESOLVED) {
            throw new IllegalArgumentException("Reconciliation %s has already been resolved.".formatted(reconciliationId));
        }

        entity.resolve(
                request.resolutionType(),
                trimToNull(request.resolutionNotes()),
                trimToNull(request.resolvedBy()),
                OffsetDateTime.now()
        );
        return toResponse(reconciliationRunRepository.save(entity));
    }

    private ReconciliationResponse toResponse(ReconciliationRunEntity entity) {
        return new ReconciliationResponse(
                entity.getId(),
                entity.getAccount().getId(),
                entity.getAccount().getAccountNumber(),
                entity.getAccount().getAccountName(),
                entity.getCurrency(),
                entity.getWindowStart(),
                entity.getWindowEnd(),
                money(entity.getLedgerBalance()),
                money(entity.getExternalBalance()),
                money(entity.getToleranceAmount()),
                money(entity.getDifferenceAmount()),
                entity.getStatus(),
                entity.getExternalReference(),
                entity.getNotes(),
                entity.getAssignedTo(),
                entity.getReviewNotes(),
                entity.getResolutionType(),
                entity.getResolutionNotes(),
                entity.getResolvedBy(),
                entity.getResolvedAt(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

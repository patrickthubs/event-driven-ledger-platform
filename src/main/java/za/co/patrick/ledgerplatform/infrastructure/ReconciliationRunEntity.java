package za.co.patrick.ledgerplatform.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import za.co.patrick.ledgerplatform.domain.ReconciliationResolutionType;
import za.co.patrick.ledgerplatform.domain.ReconciliationStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_run")
public class ReconciliationRunEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private LedgerAccountEntity account;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "window_start", nullable = false)
    private OffsetDateTime windowStart;

    @Column(name = "window_end", nullable = false)
    private OffsetDateTime windowEnd;

    @Column(name = "ledger_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal ledgerBalance;

    @Column(name = "external_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal externalBalance;

    @Column(name = "tolerance_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal toleranceAmount;

    @Column(name = "difference_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal differenceAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReconciliationStatus status;

    @Column(name = "external_reference", length = 120)
    private String externalReference;

    @Column(length = 300)
    private String notes;

    @Column(name = "assigned_to", length = 120)
    private String assignedTo;

    @Column(name = "review_notes", length = 300)
    private String reviewNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type", length = 40)
    private ReconciliationResolutionType resolutionType;

    @Column(name = "resolution_notes", length = 300)
    private String resolutionNotes;

    @Column(name = "resolved_by", length = 120)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    protected ReconciliationRunEntity() {
    }

    public ReconciliationRunEntity(
            UUID id,
            LedgerAccountEntity account,
            String currency,
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd,
            BigDecimal ledgerBalance,
            BigDecimal externalBalance,
            BigDecimal toleranceAmount,
            BigDecimal differenceAmount,
            ReconciliationStatus status,
            String externalReference,
            String notes,
            String assignedTo,
            String reviewNotes,
            ReconciliationResolutionType resolutionType,
            String resolutionNotes,
            String resolvedBy,
            OffsetDateTime resolvedAt,
            OffsetDateTime createdAt,
            OffsetDateTime completedAt
    ) {
        this.id = id;
        this.account = account;
        this.currency = currency;
        this.windowStart = windowStart;
        this.windowEnd = windowEnd;
        this.ledgerBalance = ledgerBalance;
        this.externalBalance = externalBalance;
        this.toleranceAmount = toleranceAmount;
        this.differenceAmount = differenceAmount;
        this.status = status;
        this.externalReference = externalReference;
        this.notes = notes;
        this.assignedTo = assignedTo;
        this.reviewNotes = reviewNotes;
        this.resolutionType = resolutionType;
        this.resolutionNotes = resolutionNotes;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public UUID getId() {
        return id;
    }

    public LedgerAccountEntity getAccount() {
        return account;
    }

    public String getCurrency() {
        return currency;
    }

    public OffsetDateTime getWindowStart() {
        return windowStart;
    }

    public OffsetDateTime getWindowEnd() {
        return windowEnd;
    }

    public BigDecimal getLedgerBalance() {
        return ledgerBalance;
    }

    public BigDecimal getExternalBalance() {
        return externalBalance;
    }

    public BigDecimal getToleranceAmount() {
        return toleranceAmount;
    }

    public BigDecimal getDifferenceAmount() {
        return differenceAmount;
    }

    public ReconciliationStatus getStatus() {
        return status;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getNotes() {
        return notes;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public ReconciliationResolutionType getResolutionType() {
        return resolutionType;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void moveToReview(String assignedTo, String reviewNotes) {
        this.status = ReconciliationStatus.IN_REVIEW;
        this.assignedTo = assignedTo;
        this.reviewNotes = reviewNotes;
    }

    public void resolve(
            ReconciliationResolutionType resolutionType,
            String resolutionNotes,
            String resolvedBy,
            OffsetDateTime resolvedAt
    ) {
        this.status = ReconciliationStatus.RESOLVED;
        this.resolutionType = resolutionType;
        this.resolutionNotes = resolutionNotes;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = resolvedAt;
        this.completedAt = resolvedAt;
    }
}

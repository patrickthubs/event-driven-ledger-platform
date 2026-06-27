package za.co.patrick.ledgerplatform.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import za.co.patrick.ledgerplatform.domain.JournalEntryStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "journal_entry")
public class JournalEntryEntity {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120)
    private String idempotencyKey;

    @Column(name = "external_reference", length = 120)
    private String externalReference;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "effective_at", nullable = false)
    private OffsetDateTime effectiveAt;

    @Column(name = "total_debit", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalDebit;

    @Column(name = "total_credit", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCredit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JournalEntryStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "reversal_of_journal_entry_id")
    private JournalEntryEntity reversalOfJournalEntry;

    @Column(name = "reversal_reason", length = 200)
    private String reversalReason;

    @OneToMany(mappedBy = "journalEntry")
    private List<JournalEntryLineEntity> lines = new ArrayList<>();

    protected JournalEntryEntity() {
    }

    public JournalEntryEntity(
            UUID id,
            String idempotencyKey,
            String externalReference,
            String description,
            String currency,
            OffsetDateTime effectiveAt,
            BigDecimal totalDebit,
            BigDecimal totalCredit,
            JournalEntryStatus status,
            OffsetDateTime createdAt,
            JournalEntryEntity reversalOfJournalEntry,
            String reversalReason
    ) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.externalReference = externalReference;
        this.description = description;
        this.currency = currency;
        this.effectiveAt = effectiveAt;
        this.totalDebit = totalDebit;
        this.totalCredit = totalCredit;
        this.status = status;
        this.createdAt = createdAt;
        this.reversalOfJournalEntry = reversalOfJournalEntry;
        this.reversalReason = reversalReason;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public String getDescription() {
        return description;
    }

    public String getCurrency() {
        return currency;
    }

    public OffsetDateTime getEffectiveAt() {
        return effectiveAt;
    }

    public BigDecimal getTotalDebit() {
        return totalDebit;
    }

    public BigDecimal getTotalCredit() {
        return totalCredit;
    }

    public JournalEntryStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public JournalEntryEntity getReversalOfJournalEntry() {
        return reversalOfJournalEntry;
    }

    public String getReversalReason() {
        return reversalReason;
    }

    public List<JournalEntryLineEntity> getLines() {
        return lines;
    }

    public void setLines(List<JournalEntryLineEntity> lines) {
        this.lines = lines;
    }
}

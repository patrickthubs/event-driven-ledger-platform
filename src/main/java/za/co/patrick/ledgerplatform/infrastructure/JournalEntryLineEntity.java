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
import za.co.patrick.ledgerplatform.domain.EntryDirection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "journal_entry_line")
public class JournalEntryLineEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntryEntity journalEntry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private LedgerAccountEntity account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EntryDirection direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 200)
    private String narrative;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected JournalEntryLineEntity() {
    }

    public JournalEntryLineEntity(
            UUID id,
            JournalEntryEntity journalEntry,
            LedgerAccountEntity account,
            EntryDirection direction,
            BigDecimal amount,
            String narrative,
            OffsetDateTime createdAt
    ) {
        this.id = id;
        this.journalEntry = journalEntry;
        this.account = account;
        this.direction = direction;
        this.amount = amount;
        this.narrative = narrative;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public JournalEntryEntity getJournalEntry() {
        return journalEntry;
    }

    public LedgerAccountEntity getAccount() {
        return account;
    }

    public EntryDirection getDirection() {
        return direction;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNarrative() {
        return narrative;
    }
}

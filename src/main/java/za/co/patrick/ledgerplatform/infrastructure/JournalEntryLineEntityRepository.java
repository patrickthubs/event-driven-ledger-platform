package za.co.patrick.ledgerplatform.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface JournalEntryLineEntityRepository extends JpaRepository<JournalEntryLineEntity, UUID> {

    @Query(value = """
            select
                coalesce(sum(case when l.direction = 'DEBIT' then l.amount else 0 end), 0) as totalDebits,
                coalesce(sum(case when l.direction = 'CREDIT' then l.amount else 0 end), 0) as totalCredits
            from journal_entry_line l
            join journal_entry j on j.id = l.journal_entry_id
            where l.account_id = :accountId
              and j.status = 'POSTED'
            """, nativeQuery = true)
    AccountPostingTotalsView summarizeAccount(@Param("accountId") UUID accountId);

    @Query("""
            select l
            from JournalEntryLineEntity l
            join fetch l.journalEntry j
            join fetch l.account a
            where a.id = :accountId
              and j.status = za.co.patrick.ledgerplatform.domain.JournalEntryStatus.POSTED
              and j.effectiveAt >= :from
              and j.effectiveAt <= :to
            order by j.effectiveAt asc, j.createdAt asc, l.createdAt asc
            """)
    List<JournalEntryLineEntity> findStatementLines(
            @Param("accountId") UUID accountId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query(value = """
            select
                coalesce(sum(case when l.direction = 'DEBIT' then l.amount else 0 end), 0) as totalDebits,
                coalesce(sum(case when l.direction = 'CREDIT' then l.amount else 0 end), 0) as totalCredits
            from journal_entry_line l
            join journal_entry j on j.id = l.journal_entry_id
            where l.account_id = :accountId
              and j.status = 'POSTED'
              and j.effective_at < :before
            """, nativeQuery = true)
    AccountPostingTotalsView summarizeAccountBefore(
            @Param("accountId") UUID accountId,
            @Param("before") OffsetDateTime before
    );
}

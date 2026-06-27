package za.co.patrick.ledgerplatform.infrastructure;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEntryEntityRepository extends JpaRepository<JournalEntryEntity, UUID> {

    Optional<JournalEntryEntity> findByIdempotencyKey(String idempotencyKey);

    boolean existsByReversalOfJournalEntry_Id(UUID journalEntryId);

    @EntityGraph(attributePaths = {"lines", "lines.account", "reversalOfJournalEntry"})
    List<JournalEntryEntity> findAllByOrderByEffectiveAtDescCreatedAtDesc();

    @EntityGraph(attributePaths = {"lines", "lines.account", "reversalOfJournalEntry"})
    List<JournalEntryEntity> findAllByExternalReferenceOrderByEffectiveAtDescCreatedAtDesc(String externalReference);

    @Query("""
            select distinct j
            from JournalEntryEntity j
            join j.lines l
            left join fetch j.lines fetchedLines
            left join fetch fetchedLines.account
            left join fetch j.reversalOfJournalEntry
            where l.account.id = :accountId
            order by j.effectiveAt desc, j.createdAt desc
            """)
    List<JournalEntryEntity> findAllByAccountId(@Param("accountId") UUID accountId);

    @EntityGraph(attributePaths = {"lines", "lines.account"})
    Optional<JournalEntryEntity> findById(UUID id);

    @Query("""
            select distinct j
            from JournalEntryEntity j
            left join fetch j.lines l
            left join fetch l.account
            left join fetch j.reversalOfJournalEntry
            where j.id = :journalEntryId
            """)
    Optional<JournalEntryEntity> findByIdWithLines(@Param("journalEntryId") UUID journalEntryId);
}

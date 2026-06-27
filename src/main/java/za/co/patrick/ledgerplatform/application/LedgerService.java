package za.co.patrick.ledgerplatform.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.patrick.ledgerplatform.api.AccountStatementEntryResponse;
import za.co.patrick.ledgerplatform.api.AccountStatementResponse;
import za.co.patrick.ledgerplatform.api.CreateLedgerAccountRequest;
import za.co.patrick.ledgerplatform.api.JournalEntryLineRequest;
import za.co.patrick.ledgerplatform.api.JournalEntryLineResponse;
import za.co.patrick.ledgerplatform.api.JournalEntryResponse;
import za.co.patrick.ledgerplatform.api.LedgerAccountResponse;
import za.co.patrick.ledgerplatform.api.OutboxEventResponse;
import za.co.patrick.ledgerplatform.api.PostJournalEntryRequest;
import za.co.patrick.ledgerplatform.api.ReverseJournalEntryRequest;
import za.co.patrick.ledgerplatform.api.TrialBalanceEntryResponse;
import za.co.patrick.ledgerplatform.api.TrialBalanceResponse;
import za.co.patrick.ledgerplatform.domain.AccountStatus;
import za.co.patrick.ledgerplatform.domain.AccountType;
import za.co.patrick.ledgerplatform.domain.EntryDirection;
import za.co.patrick.ledgerplatform.domain.JournalEntryStatus;
import za.co.patrick.ledgerplatform.domain.LedgerPostingCommand;
import za.co.patrick.ledgerplatform.domain.LedgerPostingValidator;
import za.co.patrick.ledgerplatform.infrastructure.AccountPostingTotalsView;
import za.co.patrick.ledgerplatform.infrastructure.JournalEntryEntity;
import za.co.patrick.ledgerplatform.infrastructure.JournalEntryEntityRepository;
import za.co.patrick.ledgerplatform.infrastructure.JournalEntryLineEntity;
import za.co.patrick.ledgerplatform.infrastructure.JournalEntryLineEntityRepository;
import za.co.patrick.ledgerplatform.infrastructure.LedgerAccountEntity;
import za.co.patrick.ledgerplatform.infrastructure.LedgerAccountEntityRepository;
import za.co.patrick.ledgerplatform.infrastructure.OutboxEventEntity;
import za.co.patrick.ledgerplatform.infrastructure.OutboxEventEntityRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LedgerService {

    private static final String JOURNAL_ENTRY_POSTED = "JOURNAL_ENTRY_POSTED";
    private static final String JOURNAL_ENTRY_REVERSED = "JOURNAL_ENTRY_REVERSED";

    private final LedgerAccountEntityRepository ledgerAccountRepository;
    private final JournalEntryEntityRepository journalEntryRepository;
    private final JournalEntryLineEntityRepository journalEntryLineRepository;
    private final OutboxEventEntityRepository outboxEventRepository;
    private final LedgerPostingValidator ledgerPostingValidator;
    private final ObjectMapper objectMapper;

    public LedgerService(
            LedgerAccountEntityRepository ledgerAccountRepository,
            JournalEntryEntityRepository journalEntryRepository,
            JournalEntryLineEntityRepository journalEntryLineRepository,
            OutboxEventEntityRepository outboxEventRepository,
            LedgerPostingValidator ledgerPostingValidator,
            ObjectMapper objectMapper
    ) {
        this.ledgerAccountRepository = ledgerAccountRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryLineRepository = journalEntryLineRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.ledgerPostingValidator = ledgerPostingValidator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LedgerAccountResponse createAccount(CreateLedgerAccountRequest request) {
        LedgerAccountEntity entity = new LedgerAccountEntity(
                UUID.randomUUID(),
                request.accountNumber().trim(),
                request.accountName().trim(),
                request.accountType(),
                request.currency().trim().toUpperCase(),
                request.allowNegativeBalance(),
                AccountStatus.ACTIVE,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        try {
            return toLedgerAccountResponse(ledgerAccountRepository.save(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("Account number already exists.");
        }
    }

    @Transactional(readOnly = true)
    public List<LedgerAccountResponse> listAccounts() {
        return ledgerAccountRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(LedgerAccountEntity::getAccountNumber))
                .map(this::toLedgerAccountResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LedgerAccountResponse getAccount(UUID accountId) {
        return toLedgerAccountResponse(findAccount(accountId));
    }

    @Transactional(readOnly = true)
    public LedgerAccountResponse getAccountByNumber(String accountNumber) {
        return toLedgerAccountResponse(findAccountByNumber(accountNumber));
    }

    @Transactional
    public JournalPostingResult postJournalEntry(PostJournalEntryRequest request) {
        String normalizedIdempotencyKey = request.idempotencyKey().trim();
        Optional<JournalEntryEntity> existing = journalEntryRepository.findByIdempotencyKey(normalizedIdempotencyKey);
        if (existing.isPresent()) {
            return new JournalPostingResult(false, toJournalEntryResponse(existing.get()));
        }

        try {
            return createJournalEntry(request);
        } catch (DataIntegrityViolationException exception) {
            JournalEntryEntity persistedExisting = journalEntryRepository.findByIdempotencyKey(normalizedIdempotencyKey)
                    .orElseThrow(() -> exception);
            return new JournalPostingResult(false, toJournalEntryResponse(persistedExisting));
        }
    }

    @Transactional(readOnly = true)
    public JournalEntryResponse getJournalEntry(UUID journalEntryId) {
        JournalEntryEntity entity = journalEntryRepository.findByIdWithLines(journalEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry %s was not found.".formatted(journalEntryId)));
        return toJournalEntryResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<JournalEntryResponse> listJournalEntries(UUID accountId, String externalReference) {
        if (accountId != null && externalReference != null) {
            throw new IllegalArgumentException("Filter by account id or external reference, not both.");
        }
        List<JournalEntryEntity> entries;
        if (accountId != null) {
            findAccount(accountId);
            entries = journalEntryRepository.findAllByAccountId(accountId);
        } else if (externalReference != null && !externalReference.isBlank()) {
            entries = journalEntryRepository.findAllByExternalReferenceOrderByEffectiveAtDescCreatedAtDesc(externalReference.trim());
        } else {
            entries = journalEntryRepository.findAllByOrderByEffectiveAtDescCreatedAtDesc();
        }
        return entries.stream()
                .map(this::toJournalEntryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<JournalEntryResponse> listJournalEntriesForAccount(UUID accountId) {
        findAccount(accountId);
        return journalEntryRepository.findAllByAccountId(accountId).stream()
                .map(this::toJournalEntryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountStatementResponse getAccountStatement(UUID accountId, OffsetDateTime from, OffsetDateTime to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Statement start date must be before or equal to the end date.");
        }
        LedgerAccountEntity account = findAccount(accountId);
        BigDecimal openingBalance = currentBalanceBefore(account, from);
        BigDecimal runningBalance = openingBalance;

        List<AccountStatementEntryResponse> entries = new ArrayList<>();
        for (JournalEntryLineEntity line : journalEntryLineRepository.findStatementLines(accountId, from, to)) {
            runningBalance = money(runningBalance.add(signedImpact(account.getAccountType(), line)));
            entries.add(new AccountStatementEntryResponse(
                    line.getJournalEntry().getId(),
                    line.getId(),
                    line.getJournalEntry().getEffectiveAt(),
                    line.getJournalEntry().getExternalReference(),
                    line.getJournalEntry().getDescription(),
                    line.getDirection(),
                    money(line.getAmount()),
                    line.getNarrative(),
                    runningBalance
            ));
        }

        return new AccountStatementResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountName(),
                account.getCurrency(),
                from,
                to,
                money(openingBalance),
                money(runningBalance),
                entries
        );
    }

    @Transactional(readOnly = true)
    public List<OutboxEventResponse> listOutboxEvents() {
        return listOutboxEvents(null);
    }

    @Transactional(readOnly = true)
    public List<OutboxEventResponse> listOutboxEvents(Boolean published) {
        List<OutboxEventEntity> events;
        if (published == null) {
            events = outboxEventRepository.findAllByOrderByCreatedAtDesc();
        } else if (published) {
            events = outboxEventRepository.findAllByPublishedAtIsNotNullOrderByPublishedAtDescCreatedAtDesc();
        } else {
            events = outboxEventRepository.findAllByPublishedAtIsNullOrderByCreatedAtAsc();
        }
        return events.stream()
                .map(this::toOutboxEventResponse)
                .toList();
    }

    @Transactional
    public OutboxEventResponse markOutboxEventPublished(UUID eventId) {
        OutboxEventEntity event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Outbox event %s was not found.".formatted(eventId)));
        if (event.getPublishedAt() != null) {
            throw new IllegalArgumentException("Outbox event %s has already been marked as published.".formatted(eventId));
        }
        event.markPublished(OffsetDateTime.now());
        return toOutboxEventResponse(outboxEventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public TrialBalanceResponse getTrialBalance(String currency) {
        String normalizedCurrency = normalizeCurrencyFilter(currency);
        List<TrialBalanceEntryResponse> entries = ledgerAccountRepository.findAll().stream()
                .filter(account -> normalizedCurrency == null || account.getCurrency().equalsIgnoreCase(normalizedCurrency))
                .sorted(Comparator.comparing(LedgerAccountEntity::getAccountNumber))
                .map(this::toTrialBalanceEntry)
                .toList();

        BigDecimal totalDebits = entries.stream()
                .map(TrialBalanceEntryResponse::debitBalance)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
        BigDecimal totalCredits = entries.stream()
                .map(TrialBalanceEntryResponse::creditBalance)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);

        return new TrialBalanceResponse(
                normalizedCurrency,
                OffsetDateTime.now(),
                money(totalDebits),
                money(totalCredits),
                entries
        );
    }

    @Transactional
    public JournalPostingResult reverseJournalEntry(UUID journalEntryId, ReverseJournalEntryRequest request) {
        JournalEntryEntity original = journalEntryRepository.findByIdWithLines(journalEntryId)
                .orElseThrow(() -> new ResourceNotFoundException("Journal entry %s was not found.".formatted(journalEntryId)));
        if (original.getReversalOfJournalEntry() != null) {
            throw new IllegalArgumentException("Reversal entries cannot be reversed again.");
        }
        if (journalEntryRepository.existsByReversalOfJournalEntry_Id(journalEntryId)) {
            throw new IllegalArgumentException("Journal entry %s has already been reversed.".formatted(journalEntryId));
        }

        String normalizedIdempotencyKey = request.idempotencyKey().trim();
        Optional<JournalEntryEntity> existing = journalEntryRepository.findByIdempotencyKey(normalizedIdempotencyKey);
        if (existing.isPresent()) {
            return new JournalPostingResult(false, toJournalEntryResponse(existing.get()));
        }

        LedgerPostingCommand reversalCommand = new LedgerPostingCommand(
                normalizedIdempotencyKey,
                original.getExternalReference(),
                "Reversal of %s: %s".formatted(original.getId(), request.reason().trim()),
                original.getCurrency(),
                OffsetDateTime.now(),
                original.getLines().stream()
                        .map(line -> new LedgerPostingCommand.LedgerPostingLine(
                                line.getAccount().getId(),
                                opposite(line.getDirection()),
                                line.getAmount(),
                                "Reversal: %s".formatted(
                                        line.getNarrative() == null || line.getNarrative().isBlank()
                                                ? request.reason().trim()
                                                : line.getNarrative()
                                )
                        ))
                        .toList()
        );

        try {
            return createJournalEntry(reversalCommand, original, request.reason().trim(), JOURNAL_ENTRY_REVERSED);
        } catch (DataIntegrityViolationException exception) {
            JournalEntryEntity persistedExisting = journalEntryRepository.findByIdempotencyKey(normalizedIdempotencyKey)
                    .orElseThrow(() -> exception);
            return new JournalPostingResult(false, toJournalEntryResponse(persistedExisting));
        }
    }

    private JournalPostingResult createJournalEntry(PostJournalEntryRequest request) {
        return createJournalEntry(new LedgerPostingCommand(
                request.idempotencyKey().trim(),
                trimToNull(request.externalReference()),
                request.description().trim(),
                request.currency().trim().toUpperCase(),
                request.effectiveAt(),
                request.lines().stream()
                        .map(this::toCommandLine)
                        .toList()
        ), null, null, JOURNAL_ENTRY_POSTED);
    }

    private JournalPostingResult createJournalEntry(
            LedgerPostingCommand command,
            JournalEntryEntity reversalOf,
            String reversalReason,
            String outboxEventType
    ) {
        ledgerPostingValidator.validate(command);

        Map<UUID, LedgerAccountEntity> accountsById = ledgerAccountRepository.findAllById(
                        command.lines().stream().map(LedgerPostingCommand.LedgerPostingLine::accountId).toList()
                ).stream()
                .collect(Collectors.toMap(LedgerAccountEntity::getId, Function.identity()));

        long distinctAccountCount = command.lines().stream()
                .map(LedgerPostingCommand.LedgerPostingLine::accountId)
                .distinct()
                .count();
        if (accountsById.size() != distinctAccountCount) {
            throw new ResourceNotFoundException("One or more accounts referenced by the journal entry do not exist.");
        }
        validateAccountsForPosting(command, accountsById);

        JournalEntryEntity journalEntry = journalEntryRepository.save(new JournalEntryEntity(
                UUID.randomUUID(),
                command.idempotencyKey(),
                command.externalReference(),
                command.description(),
                command.currency(),
                command.effectiveAt(),
                totalForDirection(command, EntryDirection.DEBIT),
                totalForDirection(command, EntryDirection.CREDIT),
                JournalEntryStatus.POSTED,
                OffsetDateTime.now(),
                reversalOf,
                reversalReason
        ));

        List<JournalEntryLineEntity> lines = command.lines().stream()
                .map(line -> new JournalEntryLineEntity(
                        UUID.randomUUID(),
                        journalEntry,
                        accountsById.get(line.accountId()),
                        line.direction(),
                        money(line.amount()),
                        trimToNull(line.narrative()),
                        OffsetDateTime.now()
                ))
                .toList();
        journalEntryLineRepository.saveAll(lines);
        journalEntry.setLines(lines);

        outboxEventRepository.save(new OutboxEventEntity(
                UUID.randomUUID(),
                "JOURNAL_ENTRY",
                journalEntry.getId(),
                outboxEventType,
                serializeEvent(new JournalEntryPostedEvent(
                        journalEntry.getId(),
                        journalEntry.getIdempotencyKey(),
                        journalEntry.getExternalReference(),
                        journalEntry.getCurrency(),
                        journalEntry.getEffectiveAt(),
                        journalEntry.getTotalDebit(),
                        journalEntry.getTotalCredit(),
                        journalEntry.getLines().size(),
                        reversalOf == null ? null : reversalOf.getId(),
                        reversalReason
                )),
                OffsetDateTime.now(),
                null
        ));

        return new JournalPostingResult(true, toJournalEntryResponse(journalEntry));
    }

    private void validateAccountsForPosting(
            LedgerPostingCommand command,
            Map<UUID, LedgerAccountEntity> accountsById
    ) {
        Map<UUID, BigDecimal> entryImpactByAccount = command.lines().stream()
                .collect(Collectors.groupingBy(
                        LedgerPostingCommand.LedgerPostingLine::accountId,
                        Collectors.reducing(
                                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                                line -> signedImpact(accountsById.get(line.accountId()).getAccountType(), line),
                                BigDecimal::add
                        )
                ));

        for (LedgerPostingCommand.LedgerPostingLine line : command.lines()) {
            LedgerAccountEntity account = accountsById.get(line.accountId());
            if (account.getStatus() != AccountStatus.ACTIVE) {
                throw new IllegalArgumentException("Account %s is not active.".formatted(account.getAccountNumber()));
            }
            if (!account.getCurrency().equalsIgnoreCase(command.currency())) {
                throw new IllegalArgumentException("All journal lines must use accounts with the same currency as the journal entry.");
            }
        }
        for (Map.Entry<UUID, BigDecimal> entry : entryImpactByAccount.entrySet()) {
            LedgerAccountEntity account = accountsById.get(entry.getKey());
            BigDecimal projectedBalance = currentBalance(account).add(entry.getValue());
            if (!account.isAllowNegativeBalance() && projectedBalance.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Account %s cannot go negative.".formatted(account.getAccountNumber()));
            }
        }
    }

    private LedgerPostingCommand.LedgerPostingLine toCommandLine(JournalEntryLineRequest line) {
        return new LedgerPostingCommand.LedgerPostingLine(
                line.accountId(),
                line.direction(),
                line.amount(),
                line.narrative()
        );
    }

    private BigDecimal totalForDirection(LedgerPostingCommand command, EntryDirection direction) {
        return command.lines().stream()
                .filter(line -> line.direction() == direction)
                .map(LedgerPostingCommand.LedgerPostingLine::amount)
                .map(this::money)
                .reduce(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), BigDecimal::add);
    }

    private LedgerAccountEntity findAccount(UUID accountId) {
        return ledgerAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account %s was not found.".formatted(accountId)));
    }

    private LedgerAccountEntity findAccountByNumber(String accountNumber) {
        return ledgerAccountRepository.findByAccountNumber(accountNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Account %s was not found.".formatted(accountNumber)));
    }

    private LedgerAccountResponse toLedgerAccountResponse(LedgerAccountEntity entity) {
        BigDecimal balance = currentBalance(entity);
        return new LedgerAccountResponse(
                entity.getId(),
                entity.getAccountNumber(),
                entity.getAccountName(),
                entity.getAccountType(),
                entity.getCurrency(),
                entity.isAllowNegativeBalance(),
                entity.getStatus(),
                balance,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private BigDecimal currentBalance(LedgerAccountEntity entity) {
        return calculateBalance(entity, journalEntryLineRepository.summarizeAccount(entity.getId()));
    }

    private BigDecimal currentBalanceBefore(LedgerAccountEntity entity, OffsetDateTime before) {
        return calculateBalance(entity, journalEntryLineRepository.summarizeAccountBefore(entity.getId(), before));
    }

    private JournalEntryResponse toJournalEntryResponse(JournalEntryEntity entity) {
        JournalEntryEntity hydratedEntity = entity.getLines() == null || entity.getLines().isEmpty()
                ? journalEntryRepository.findByIdWithLines(entity.getId()).orElse(entity)
                : entity;
        List<JournalEntryLineResponse> lines = hydratedEntity.getLines().stream()
                .map(line -> new JournalEntryLineResponse(
                        line.getId(),
                        line.getAccount().getId(),
                        line.getAccount().getAccountNumber(),
                        line.getAccount().getAccountName(),
                        line.getDirection(),
                        money(line.getAmount()),
                        line.getNarrative()
                ))
                .toList();
        return new JournalEntryResponse(
                hydratedEntity.getId(),
                hydratedEntity.getIdempotencyKey(),
                hydratedEntity.getExternalReference(),
                hydratedEntity.getDescription(),
                hydratedEntity.getCurrency(),
                hydratedEntity.getEffectiveAt(),
                money(hydratedEntity.getTotalDebit()),
                money(hydratedEntity.getTotalCredit()),
                hydratedEntity.getStatus(),
                hydratedEntity.getCreatedAt(),
                hydratedEntity.getReversalOfJournalEntry() == null ? null : hydratedEntity.getReversalOfJournalEntry().getId(),
                hydratedEntity.getReversalReason(),
                lines
        );
    }

    private OutboxEventResponse toOutboxEventResponse(OutboxEventEntity entity) {
        return new OutboxEventResponse(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getCreatedAt(),
                entity.getPublishedAt()
        );
    }

    private TrialBalanceEntryResponse toTrialBalanceEntry(LedgerAccountEntity entity) {
        BigDecimal balance = currentBalance(entity);
        BigDecimal absoluteBalance = money(balance.abs());
        if (balance.compareTo(BigDecimal.ZERO) == 0) {
            return new TrialBalanceEntryResponse(
                    entity.getId(),
                    entity.getAccountNumber(),
                    entity.getAccountName(),
                    entity.getAccountType(),
                    entity.getAccountType().normalBalanceSide(),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            );
        }

        EntryDirection balanceSide = balance.signum() >= 0
                ? entity.getAccountType().normalBalanceSide()
                : opposite(entity.getAccountType().normalBalanceSide());
        BigDecimal debitBalance = balanceSide == EntryDirection.DEBIT
                ? absoluteBalance
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal creditBalance = balanceSide == EntryDirection.CREDIT
                ? absoluteBalance
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new TrialBalanceEntryResponse(
                entity.getId(),
                entity.getAccountNumber(),
                entity.getAccountName(),
                entity.getAccountType(),
                balanceSide,
                debitBalance,
                creditBalance
        );
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBalance(LedgerAccountEntity entity, AccountPostingTotalsView totals) {
        BigDecimal debits = totals == null || totals.getTotalDebits() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : money(totals.getTotalDebits());
        BigDecimal credits = totals == null || totals.getTotalCredits() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : money(totals.getTotalCredits());
        BigDecimal balance = entity.getAccountType().normalBalanceSide() == EntryDirection.DEBIT
                ? debits.subtract(credits)
                : credits.subtract(debits);
        return money(balance);
    }

    private String serializeEvent(JournalEntryPostedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbox event payload.", exception);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeCurrencyFilter(String currency) {
        String normalized = trimToNull(currency);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private record JournalEntryPostedEvent(
            UUID journalEntryId,
            String idempotencyKey,
            String externalReference,
            String currency,
            OffsetDateTime effectiveAt,
            BigDecimal totalDebit,
            BigDecimal totalCredit,
            int lineCount,
            UUID reversalOfJournalEntryId,
            String reversalReason
    ) {
    }

    private BigDecimal signedImpact(AccountType accountType, LedgerPostingCommand.LedgerPostingLine line) {
        BigDecimal amount = money(line.amount());
        if (accountType.normalBalanceSide() == EntryDirection.DEBIT) {
            return line.direction() == EntryDirection.DEBIT ? amount : amount.negate();
        }
        return line.direction() == EntryDirection.CREDIT ? amount : amount.negate();
    }

    private BigDecimal signedImpact(AccountType accountType, JournalEntryLineEntity line) {
        BigDecimal amount = money(line.getAmount());
        if (accountType.normalBalanceSide() == EntryDirection.DEBIT) {
            return line.getDirection() == EntryDirection.DEBIT ? amount : amount.negate();
        }
        return line.getDirection() == EntryDirection.CREDIT ? amount : amount.negate();
    }

    private EntryDirection opposite(EntryDirection direction) {
        return direction == EntryDirection.DEBIT ? EntryDirection.CREDIT : EntryDirection.DEBIT;
    }
}

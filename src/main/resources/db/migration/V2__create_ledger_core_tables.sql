create table ledger_account (
    id uuid primary key,
    account_number varchar(50) not null unique,
    account_name varchar(120) not null,
    account_type varchar(20) not null,
    currency varchar(3) not null,
    allow_negative_balance boolean not null default false,
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table journal_entry (
    id uuid primary key,
    idempotency_key varchar(120) not null unique,
    external_reference varchar(120),
    description varchar(200) not null,
    currency varchar(3) not null,
    effective_at timestamp with time zone not null,
    total_debit numeric(19, 2) not null,
    total_credit numeric(19, 2) not null,
    status varchar(20) not null,
    created_at timestamp with time zone not null
);

create table journal_entry_line (
    id uuid primary key,
    journal_entry_id uuid not null references journal_entry(id),
    account_id uuid not null references ledger_account(id),
    direction varchar(10) not null,
    amount numeric(19, 2) not null,
    narrative varchar(200),
    created_at timestamp with time zone not null
);

create table outbox_event (
    id uuid primary key,
    aggregate_type varchar(80) not null,
    aggregate_id uuid not null,
    event_type varchar(80) not null,
    payload text not null,
    created_at timestamp with time zone not null,
    published_at timestamp with time zone
);

create index idx_journal_entry_effective_at on journal_entry(effective_at);
create index idx_journal_entry_line_account on journal_entry_line(account_id);
create index idx_journal_entry_line_journal on journal_entry_line(journal_entry_id);
create index idx_outbox_event_created_at on outbox_event(created_at);

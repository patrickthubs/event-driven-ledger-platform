create table reconciliation_run (
    id uuid primary key,
    account_id uuid not null references ledger_account(id),
    currency varchar(3) not null,
    window_start timestamp with time zone not null,
    window_end timestamp with time zone not null,
    ledger_balance numeric(19, 2) not null,
    external_balance numeric(19, 2) not null,
    difference_amount numeric(19, 2) not null,
    status varchar(20) not null,
    external_reference varchar(120),
    notes varchar(300),
    created_at timestamp with time zone not null,
    completed_at timestamp with time zone not null
);

create index idx_reconciliation_run_account_created_at
    on reconciliation_run(account_id, created_at desc);

create index idx_reconciliation_run_status
    on reconciliation_run(status);

create table ledger_bootstrap_marker (
    id uuid primary key,
    marker_name varchar(100) not null unique,
    created_at timestamp not null
);

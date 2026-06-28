alter table reconciliation_run
    add column tolerance_amount numeric(19, 2) not null default 0.00;

alter table reconciliation_run
    add column assigned_to varchar(120);

alter table reconciliation_run
    add column review_notes varchar(300);

alter table reconciliation_run
    add column resolution_type varchar(40);

alter table reconciliation_run
    add column resolution_notes varchar(300);

alter table reconciliation_run
    add column resolved_by varchar(120);

alter table reconciliation_run
    add column resolved_at timestamp with time zone;

alter table reconciliation_run
    alter column completed_at drop not null;

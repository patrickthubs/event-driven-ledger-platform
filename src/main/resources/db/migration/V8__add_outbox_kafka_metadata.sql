alter table outbox_event
    add column destination_topic varchar(120) not null default 'ledger.journal.entries';

alter table outbox_event
    add column message_key varchar(120) not null default 'UNKNOWN';

alter table outbox_event
    add column published_partition integer;

alter table outbox_event
    add column published_offset bigint;

create index idx_outbox_event_destination_topic_published_at
    on outbox_event(destination_topic, published_at, created_at);

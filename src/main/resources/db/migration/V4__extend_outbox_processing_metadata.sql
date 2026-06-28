alter table outbox_event
    add column last_attempted_at timestamp with time zone;

alter table outbox_event
    add column publish_attempt_count integer not null default 0;

alter table outbox_event
    add column last_publish_error varchar(500);

create index idx_outbox_event_published_created_at
    on outbox_event(published_at, created_at);

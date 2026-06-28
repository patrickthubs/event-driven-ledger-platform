create table app_user (
    id uuid primary key,
    username varchar(80) not null unique,
    password_hash varchar(100) not null,
    enabled boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table app_user_role (
    user_id uuid not null references app_user(id) on delete cascade,
    role_name varchar(30) not null,
    primary key (user_id, role_name)
);

create index idx_app_user_username
    on app_user(username);

insert into app_user (id, username, password_hash, enabled, created_at, updated_at)
values
    ('11111111-1111-1111-1111-111111111111', 'admin', '$2a$10$5VZWJoNM.bmy6ICpsXcf9.Jfc1rG7FW3AuvaC4.hjzHbgm8Ti5WAe', true, current_timestamp, current_timestamp),
    ('22222222-2222-2222-2222-222222222222', 'auditor', '$2a$10$mE9LEUV3.aah5P.zrPt2mOxqPjyT9qCQkiH6tvmzHntg5esrvuG4q', true, current_timestamp, current_timestamp),
    ('33333333-3333-3333-3333-333333333333', 'operator', '$2a$10$9s5BwVnwofkXot9U5eta6O74mynNAvkojGnxarRwHGKNZWife.ai6', true, current_timestamp, current_timestamp),
    ('44444444-4444-4444-4444-444444444444', 'publisher', '$2a$10$d78oTkL0bIHXgAbk6AJUDO3TorrN0ROwGn/K6Klyl5cxmsWqqTz7O', true, current_timestamp, current_timestamp),
    ('55555555-5555-5555-5555-555555555555', 'reconciler', '$2a$10$Aucchc03zO6y1dXwrX88XOp5kQricOQGE5yRpEydfcCmYAv4P3t1S', true, current_timestamp, current_timestamp);

insert into app_user_role (user_id, role_name)
values
    ('11111111-1111-1111-1111-111111111111', 'ADMIN'),
    ('22222222-2222-2222-2222-222222222222', 'AUDITOR'),
    ('33333333-3333-3333-3333-333333333333', 'OPERATOR'),
    ('44444444-4444-4444-4444-444444444444', 'PUBLISHER'),
    ('55555555-5555-5555-5555-555555555555', 'RECONCILER');

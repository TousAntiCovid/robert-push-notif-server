create table push
(
    id                   bigserial    not null,
    active               boolean,
    creation_date        timestamp,
    deleted              boolean,
    failed_push_sent     int4,
    last_error_code      varchar(255),
    last_failure_push    timestamp,
    last_successful_push timestamp,
    locale               varchar(255) not null,
    next_planned_push    timestamp,
    successful_push_sent int4,
    timezone             varchar(255) not null,
    token                varchar(255) not null,
    primary key (id)
);

create index IDX_TOKEN on push (token);
create sequence push_id_seq;

create table push
(
    id                   bigint not null default nextval('push_id_seq'::regclass),
    active               boolean,
    creation_date        timestamp without time zone,
    deleted              boolean,
    failed_push_sent     integer,
    last_error_code      character varying(255),
    last_failure_push    timestamp without time zone,
    last_successful_push timestamp without time zone,
    locale               character varying(255) not null,
    next_planned_push    timestamp without time zone,
    successful_push_sent integer,
    timezone             character varying(255) not null,
    token                character varying(255) not null unique,
    primary key (id)
);

alter sequence push_id_seq owned by push.id;

create index IDX_TOKEN on push (token);
create table example(
                        id uuid primary key,
                        data varchar(256) not null
);

create table order_outbox_event(
                             id uuid primary key,
                             timestamp timestamptz not null,
                             aggregate_id uuid not null,
                             destination_topic varchar(256) not null,
                             payload jsonb not null,
                             type varchar(256) not null,
                             trace_id varchar(256) not null
);
create table warehouse_outbox_event(
                             id uuid primary key,
                             timestamp timestamptz not null,
                             aggregate_id uuid not null,
                             destination_topic varchar(256) not null,
                             payload jsonb not null,
                             type varchar(256) not null,
                             trace_id varchar(256) not null
);
create table wallet_outbox_event(
                             id uuid primary key,
                             timestamp timestamptz not null,
                             aggregate_id uuid not null,
                             destination_topic varchar(256) not null,
                             payload jsonb not null,
                             type varchar(256) not null,
                             trace_id varchar(256) not null
);

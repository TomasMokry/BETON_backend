create table marketplaces
(
    id         bigint auto_increment
        primary key,
    name       varchar(255)                       not null,
    address    varchar(500)                       null,
    type       varchar(30)                        not null,
    start_date date                               null,
    end_date   date                               null,
    active     boolean  default true              not null,
    notes      text                               null,
    created_at datetime default current_timestamp not null
);

ALTER TABLE orders
    ADD COLUMN marketplace_id bigint NULL,
    ADD CONSTRAINT orders_marketplaces_id_fk
        FOREIGN KEY (marketplace_id) REFERENCES marketplaces (id);

ALTER TABLE users
    ADD COLUMN current_marketplace_id bigint NULL,
    ADD CONSTRAINT users_marketplaces_id_fk
        FOREIGN KEY (current_marketplace_id) REFERENCES marketplaces (id) ON DELETE SET NULL;

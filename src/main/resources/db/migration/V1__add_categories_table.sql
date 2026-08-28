create table categories
(
    id   tinyint auto_increment not null ,
    name varchar(255) not null,
    constraint categories_pk
        primary key (id)
);

create table products
(
    id             bigint auto_increment,
    name           varchar(255)   not null,
    size           varchar(15)    not null,
    price          decimal(10, 2) not null,
    description    text           null,
    height         int            null,
    width          int            null,
    weight         int            null,
    length         int            null,
    color          varchar(255)   not null,
    url_image       varchar(255)   null,
    amount          int            not null,
    category_id     tinyint not null,
    constraint products_pk
        primary key (id)
);

create table users
(
    id       bigint auto_increment not null,
    name     varchar(255) not null,
    email    varchar(255) not null,
    password varchar(255) not null,
    constraint `primary` primary key (id)
);

alter table products add constraint fk_category
        foreign key (category_id) references categories (id) on delete no action;

create index fk_category on products (category_id);

alter table users
    add role varchar(20) default 'USER' not null;

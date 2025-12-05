create table account
(
    id            int auto_increment
        primary key,
    username      varchar(255) not null,
    password      varchar(255) not null,
    email         varchar(255) not null,
    role          varchar(255) not null,
    register_time datetime     not null,
    constraint unique_email
        unique (email),
    constraint unique_name
        unique (username)
);

create table ai_models
(
    id   int auto_increment
        primary key,
    name varchar(255) not null
);

create table characters
(
    id   int auto_increment
        primary key,
    name varchar(255) not null
);

create table chat_history
(
    id           int auto_increment
        primary key,
    user_id      int      not null,
    character_id int      not null,
    model_id     int      not null,
    input        text     not null,
    output       text     null,
    begin        datetime not null,
    finish       datetime null,
    constraint character_id_fk
        foreign key (character_id) references characters (id),
    constraint models_id_fk
        foreign key (model_id) references ai_models (id),
    constraint user_id_fk
        foreign key (user_id) references account (id)
)
    collate = utf8mb4_unicode_ci;

create index character_id
    on chat_history (character_id);

create index model_id
    on chat_history (model_id);

create index user_id
    on chat_history (user_id);

INSERT INTO web_ai_chat.account (id, username, password, email, role, register_time) VALUES (1, 'test', '$2a$10$Xqe2W.5zs/I8FAs3cK331e6tyuo4XWTHNhW1oV6bOHUzwMZ2kAPTa', '1234567890@gmail.com', 'user', '2025-11-06 15:56:37');

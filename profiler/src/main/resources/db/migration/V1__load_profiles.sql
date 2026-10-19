-- Профиль хранится целиком как JSONB-документ: он читается и пишется
-- как один артефакт. name и created_at продублированы обычными колонками,
-- чтобы списки профилей выбирались без разбора JSON.
create table load_profiles (
    id         bigint generated always as identity primary key,
    name       text        not null,
    created_at timestamptz not null,
    payload    jsonb       not null
);

create index load_profiles_created_at on load_profiles (created_at);

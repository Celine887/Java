--liquibase formatted sql

--changeset dsibilev:1
--comment: Create chats table
CREATE TABLE IF NOT EXISTS chats (
    id BIGINT PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--changeset dsibilev:2
--comment: Create links table
CREATE TABLE IF NOT EXISTS links (
    id BIGSERIAL PRIMARY KEY,
    url TEXT NOT NULL UNIQUE,
    last_updated_at TIMESTAMP WITH TIME ZONE,
    last_checked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--changeset dsibilev:3
--comment: Create link_chat junction table
CREATE TABLE IF NOT EXISTS link_chat (
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    chat_id BIGINT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    subscribed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (link_id, chat_id)
);

--changeset dsibilev:4
--comment: Create tags table
CREATE TABLE IF NOT EXISTS tags (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--changeset dsibilev:5
--comment: Create link_tag junction table
CREATE TABLE IF NOT EXISTS link_tag (
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (link_id, tag_id)
);

--changeset dsibilev:6
--comment: Create indexes for performance optimization
CREATE INDEX IF NOT EXISTS idx_links_url ON links(url);
CREATE INDEX IF NOT EXISTS idx_links_last_checked_at ON links(last_checked_at);
CREATE INDEX IF NOT EXISTS idx_link_chat_chat_id ON link_chat(chat_id);
CREATE INDEX IF NOT EXISTS idx_link_chat_link_id ON link_chat(link_id);
CREATE INDEX IF NOT EXISTS idx_link_tag_link_id ON link_tag(link_id);
CREATE INDEX IF NOT EXISTS idx_link_tag_tag_id ON link_tag(tag_id);
CREATE INDEX IF NOT EXISTS idx_tags_name ON tags(name);

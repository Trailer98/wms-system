CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 可选：给 Spring AI 手动建表。
-- 如果你准备让 Spring AI 自动初始化 schema，可以不建这个表。
CREATE TABLE IF NOT EXISTS vector_store (
                                            id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1536)
    );

CREATE INDEX IF NOT EXISTS vector_store_embedding_hnsw_idx
    ON vector_store
    USING HNSW (embedding vector_cosine_ops);
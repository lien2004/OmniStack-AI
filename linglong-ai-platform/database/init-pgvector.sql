-- =============================================
-- 玲珑AI平台 - PostgreSQL pgvector 扩展初始化脚本
-- 向量维度：1024（智谱AI embedding-3）
-- =============================================

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- =============================================
-- 方案：直接使用物理表 vector_store
-- 原因：PgVectorStore 使用 ON CONFLICT 语法，PostgreSQL 视图规则不支持
-- =============================================

-- 步骤1：如果存在旧表 linglong，将数据迁移到 vector_store
DO $$
BEGIN
    -- 检查是否存在 linglong 表
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'linglong') THEN
        -- 检查是否存在 vector_store 表
        IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'vector_store') THEN
            -- 重命名 linglong 表为 vector_store
            ALTER TABLE linglong RENAME TO vector_store;
            RAISE NOTICE '已将 linglong 表重命名为 vector_store';
        ELSE
            -- 如果两个表都存在，将数据从 linglong 迁移到 vector_store
            INSERT INTO vector_store (id, content, metadata, embedding)
            SELECT id, content, metadata, embedding FROM linglong
            ON CONFLICT (id) DO NOTHING;
            RAISE NOTICE '已将 linglong 数据迁移到 vector_store';
        END IF;
    END IF;
END $$;

-- 步骤2：删除旧视图（如果存在）
DROP VIEW IF EXISTS vector_store CASCADE;

-- 步骤3：创建物理表 vector_store（直接使用，不使用视图）
CREATE TABLE IF NOT EXISTS vector_store (
    id        UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT,
    metadata  JSONB,
    embedding VECTOR(1024)
);

-- HNSW 索引（查询性能最佳）
CREATE INDEX IF NOT EXISTS vector_store_embedding_hnsw_idx
ON vector_store
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- IVFFlat 索引（可选，适合大规模数据，需先有数据再建）
-- CREATE INDEX IF NOT EXISTS vector_store_embedding_idx
-- ON vector_store
-- USING ivfflat (embedding vector_cosine_ops)
-- WITH (lists = 100);

-- 创建向量相似度搜索函数
CREATE OR REPLACE FUNCTION cosine_similarity_search(
    query_vector VECTOR,
    limit_count INTEGER DEFAULT 10
)
RETURNS TABLE (
    id UUID,
    content TEXT,
    metadata JSONB,
    similarity FLOAT
)
AS $$
BEGIN
    RETURN QUERY
    SELECT 
        vs.id,
        vs.content,
        vs.metadata,
        1 - (vs.embedding <=> query_vector) AS similarity
    FROM vector_store vs
    ORDER BY vs.embedding <=> query_vector
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

-- 创建全文搜索函数 (结合向量搜索)
CREATE OR REPLACE FUNCTION hybrid_search(
    query_text TEXT,
    query_vector VECTOR,
    keyword_weight FLOAT DEFAULT 0.3,
    vector_weight FLOAT DEFAULT 0.7,
    limit_count INTEGER DEFAULT 10
)
RETURNS TABLE (
    id UUID,
    content TEXT,
    metadata JSONB,
    combined_score FLOAT
)
AS $$
BEGIN
    RETURN QUERY
    WITH vector_results AS (
        SELECT 
            vs.id,
            vs.content,
            vs.metadata,
            1 - (vs.embedding <=> query_vector) AS vector_score
        FROM vector_store vs
        ORDER BY vs.embedding <=> query_vector
        LIMIT limit_count * 2
    )
    SELECT 
        vr.id,
        vr.content,
        vr.metadata,
        (keyword_weight * 0) + (vector_weight * vr.vector_score) AS combined_score
    FROM vector_results vr
    ORDER BY combined_score DESC
    LIMIT limit_count;
END;
$$ LANGUAGE plpgsql;

-- 授权
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO postgres;

DO $$ BEGIN RAISE NOTICE 'pgvector 扩展初始化完成!'; END $$;

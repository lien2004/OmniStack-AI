-- =============================================
-- 玲珑AI平台 - PostgreSQL pgvector 扩展初始化脚本
-- 向量维度：1024（智谱AI embedding-3）
-- =============================================

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- 删除旧表和视图（CASCADE 一并删除索引、规则、依赖）
-- 注意：生产环境请勿随意执行 DROP，会导致数据丢失！
-- DROP TABLE IF EXISTS linglong CASCADE;
-- DROP TABLE IF EXISTS vector_store CASCADE;

-- 创建物理表 linglong（1024 维，Spring AI 标准字段）
-- 使用 IF NOT EXISTS 避免重复创建时报错
CREATE TABLE IF NOT EXISTS linglong (
    id        UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT,
    metadata  JSONB,
    embedding VECTOR(1024)
);

-- HNSW 索引（查询性能最佳）
CREATE INDEX IF NOT EXISTS linglong_embedding_hnsw_idx
ON linglong
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- IVFFlat 索引（可选，适合大规模数据，需先有数据再建）
-- CREATE INDEX IF NOT EXISTS linglong_embedding_idx
-- ON linglong
-- USING ivfflat (embedding vector_cosine_ops)
-- WITH (lists = 100);

-- 创建视图 vector_store 指向 linglong
-- （Spring AI 0.8.x 硬编码表名为 vector_store，由视图透明转发）
-- 使用 OR REPLACE 允许重复执行
CREATE OR REPLACE VIEW vector_store AS SELECT * FROM linglong;

-- 视图 INSERT 规则
-- 先删除旧规则（如果存在），避免重复创建报错
DROP RULE IF EXISTS vector_store_insert ON vector_store;
CREATE RULE vector_store_insert AS ON INSERT TO vector_store
    DO INSTEAD
    INSERT INTO linglong (id, content, metadata, embedding)
    VALUES (NEW.id, NEW.content, NEW.metadata, NEW.embedding);

-- 视图 UPDATE 规则
-- 先删除旧规则（如果存在），避免重复创建报错
DROP RULE IF EXISTS vector_store_update ON vector_store;
CREATE RULE vector_store_update AS ON UPDATE TO vector_store
    DO INSTEAD
    UPDATE linglong
    SET content = NEW.content, metadata = NEW.metadata, embedding = NEW.embedding
    WHERE id = OLD.id;

-- 视图 DELETE 规则
-- 先删除旧规则（如果存在），避免重复创建报错
DROP RULE IF EXISTS vector_store_delete ON vector_store;
CREATE RULE vector_store_delete AS ON DELETE TO vector_store
    DO INSTEAD
    DELETE FROM linglong WHERE id = OLD.id;

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
    FROM linglong vs
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
        FROM linglong vs
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

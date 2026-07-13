package com.example.wms.admin.knowledge;

import com.example.wms.admin.model.entity.KnowledgeChunk;
import com.example.wms.admin.model.entity.KnowledgeDocument;
import com.example.wms.common.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin bridge between knowledge chunks (MySQL master data) and the pgvector index copy.
 *
 * <p>The {@link VectorStore} bean only exists under the {@code ai} profile (see {@code
 * AiVectorStoreConfig}), so it is injected as an optional {@link ObjectProvider}. When it is absent —
 * or when the embedding backend is down — vectorization records a FAILED status on the chunk and
 * search returns a clear error, rather than pretending to succeed. This keeps the knowledge-management
 * main flow unblocked when the vector link is incomplete.
 */
@Component
public class KnowledgeVectorizer {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeVectorizer.class);

    /** Metadata key holding the MySQL chunk id, so a pgvector hit can be resolved back to master data. */
    public static final String META_CHUNK_ID = "chunkId";
    public static final String META_DOC_ID = "docId";

    private final ObjectProvider<VectorStore> vectorStoreProvider;

    public KnowledgeVectorizer(ObjectProvider<VectorStore> vectorStoreProvider) {
        this.vectorStoreProvider = vectorStoreProvider;
    }

    /** A raw vector hit: the chunk id encoded in the pgvector metadata plus its similarity score. */
    public record ScoredChunk(Long chunkId, Double score) {
    }

    public boolean isAvailable() {
        return vectorStoreProvider.getIfAvailable() != null;
    }

    /**
     * Embed and upsert a single chunk into pgvector. Throws on failure so the caller can mark the chunk
     * FAILED; callers must gate on {@link #isAvailable()} first for the "not configured" case.
     */
    public void index(KnowledgeDocument document, KnowledgeChunk chunk) {
        VectorStore store = require();
        Document doc = new Document(chunk.getContent(), metadata(document, chunk));
        store.add(List.of(doc));
    }

    /** Best-effort removal of all vectors for a document. Never throws (index cleanup is not critical). */
    public void deleteByDocument(Long docId) {
        VectorStore store = vectorStoreProvider.getIfAvailable();
        if (store == null) {
            return;
        }
        try {
            Filter.Expression expression = new FilterExpressionBuilder()
                    .eq(META_DOC_ID, String.valueOf(docId))
                    .build();
            store.delete(expression);
        } catch (RuntimeException ex) {
            log.warn("delete vectors for docId={} failed (index copy may be stale): {}", docId, ex.getMessage());
        }
    }

    /**
     * Similarity search returning raw (chunkId, score) hits. {@code topN} is over-fetched by the caller
     * so module/operationType/status can be enforced live against MySQL afterwards. Throws a
     * {@link BusinessException} when the vector link is unavailable — the caller surfaces that verbatim.
     */
    public List<ScoredChunk> search(String query, int topN) {
        VectorStore store = require();
        List<Document> documents;
        try {
            documents = store.similaritySearch(SearchRequest.builder()
                    .query(query)
                    .topK(topN)
                    .build());
        } catch (RuntimeException ex) {
            log.warn("knowledge similarity search failed: {}", ex.getMessage());
            throw new BusinessException("向量检索失败：" + rootMessage(ex));
        }
        List<ScoredChunk> hits = new ArrayList<>();
        for (Document document : documents) {
            Object chunkId = document.getMetadata().get(META_CHUNK_ID);
            Long parsed = parseLong(chunkId);
            if (parsed != null) {
                hits.add(new ScoredChunk(parsed, document.getScore()));
            }
        }
        return hits;
    }

    private VectorStore require() {
        VectorStore store = vectorStoreProvider.getIfAvailable();
        if (store == null) {
            throw new BusinessException("向量检索服务当前不可用：AI 向量库未启用（未激活 ai profile 或未连接 pgvector）");
        }
        return store;
    }

    private Map<String, Object> metadata(KnowledgeDocument document, KnowledgeChunk chunk) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(META_CHUNK_ID, String.valueOf(chunk.getId()));
        metadata.put(META_DOC_ID, String.valueOf(chunk.getDocId()));
        putIfPresent(metadata, "docCode", document.getDocCode());
        putIfPresent(metadata, "title", chunk.getTitle());
        putIfPresent(metadata, "module", chunk.getModule());
        putIfPresent(metadata, "sourceType", chunk.getSourceType());
        putIfPresent(metadata, "bizType", chunk.getBizType());
        putIfPresent(metadata, "operationType", chunk.getOperationType());
        putIfPresent(metadata, "entityName", chunk.getEntityName());
        putIfPresent(metadata, "scenario", chunk.getScenario());
        putIfPresent(metadata, "keywords", chunk.getKeywords());
        putIfPresent(metadata, "version", document.getVersion());
        putIfPresent(metadata, "status", chunk.getStatus());
        return metadata;
    }

    private void putIfPresent(Map<String, Object> metadata, String key, String value) {
        if (StringUtils.hasText(value)) {
            metadata.put(key, value);
        }
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String rootMessage(Throwable ex) {
        Throwable cursor = ex;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }
}

package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.admin.knowledge.KnowledgeVectorizer;
import com.example.wms.admin.knowledge.MarkdownSemanticChunker;
import com.example.wms.admin.knowledge.MarkdownSemanticChunker.ChunkPiece;
import com.example.wms.admin.model.entity.KnowledgeChunk;
import com.example.wms.admin.model.entity.KnowledgeDocument;
import com.example.wms.admin.model.mapper.KnowledgeChunkMapper;
import com.example.wms.admin.model.mapper.KnowledgeDocumentMapper;
import com.example.wms.admin.security.CurrentUser;
import com.example.wms.admin.security.CurrentUserContext;
import com.example.wms.admin.view.dto.KnowledgeChunkQuery;
import com.example.wms.admin.view.dto.KnowledgeChunkView;
import com.example.wms.admin.view.dto.KnowledgeDocumentDetail;
import com.example.wms.admin.view.dto.KnowledgeDocumentListItem;
import com.example.wms.admin.view.dto.KnowledgeDocumentQuery;
import com.example.wms.admin.view.dto.KnowledgeDocumentSaveRequest;
import com.example.wms.admin.view.dto.KnowledgeSaveResult;
import com.example.wms.admin.view.dto.KnowledgeSearchHit;
import com.example.wms.admin.view.dto.KnowledgeSearchRequest;
import com.example.wms.admin.view.dto.KnowledgeSearchResponse;
import com.example.wms.admin.view.dto.KnowledgeVectorizeResult;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI knowledge base P0 service. MySQL {@code knowledge_document}/{@code knowledge_chunk} are the master
 * data; pgvector is only an index copy driven through {@link KnowledgeVectorizer}. Chunking is done
 * server-side by {@link MarkdownSemanticChunker}. Vectorization never blocks the management main flow:
 * when the vector link is unavailable chunks are recorded FAILED and search returns a clear error.
 */
@Service
public class KnowledgeService {

    private static final Set<String> DOCUMENT_STATUSES =
            Set.of(KnowledgeDocument.STATUS_DRAFT, KnowledgeDocument.STATUS_ENABLED, KnowledgeDocument.STATUS_DISABLED);
    private static final Set<String> CHUNK_STRATEGIES = Set.of(
            MarkdownSemanticChunker.STRATEGY_SEMANTIC_MARKDOWN,
            MarkdownSemanticChunker.STRATEGY_FIXED_SIZE,
            MarkdownSemanticChunker.STRATEGY_NONE);
    private static final Set<String> CONTENT_FORMATS = Set.of("MARKDOWN", "TEXT");
    private static final int SEARCH_DEFAULT_TOP_K = 5;
    private static final int SEARCH_MAX_TOP_K = 20;
    private static final String MODULE_DICT = "ai_knowledge_module";
    private static final String SOURCE_TYPE_DICT = "ai_knowledge_source_type";

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final MarkdownSemanticChunker chunker;
    private final KnowledgeVectorizer vectorizer;
    private final SysDictService sysDictService;

    public KnowledgeService(KnowledgeDocumentMapper documentMapper,
                            KnowledgeChunkMapper chunkMapper,
                            MarkdownSemanticChunker chunker,
                            KnowledgeVectorizer vectorizer,
                            SysDictService sysDictService) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.chunker = chunker;
        this.vectorizer = vectorizer;
        this.sysDictService = sysDictService;
    }

    // ---- documents: read ----------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public PageResponse<KnowledgeDocumentListItem> pageDocuments(KnowledgeDocumentQuery query) {
        var wrapper = Wrappers.lambdaQuery(KnowledgeDocument.class)
                .like(StringUtils.hasText(query.getTitle()), KnowledgeDocument::getTitle, query.getTitle())
                .eq(StringUtils.hasText(query.getModule()), KnowledgeDocument::getModule, query.getModule())
                .eq(StringUtils.hasText(query.getSourceType()), KnowledgeDocument::getSourceType, query.getSourceType())
                .eq(StringUtils.hasText(query.getStatus()), KnowledgeDocument::getStatus, query.getStatus());

        if (StringUtils.hasText(query.getKeyword())) {
            String kw = query.getKeyword();
            wrapper.and(w -> w.like(KnowledgeDocument::getTitle, kw)
                    .or().like(KnowledgeDocument::getDocCode, kw)
                    .or().like(KnowledgeDocument::getKeywords, kw));
        }

        if (StringUtils.hasText(query.getVectorStatus())) {
            List<Long> docIds = docIdsByChunkVectorStatus(query.getVectorStatus());
            if (docIds.isEmpty()) {
                return new PageResponse<>(query.getPageNum(), query.getPageSize(), 0, List.of());
            }
            wrapper.in(KnowledgeDocument::getId, docIds);
        }

        wrapper.orderByDesc(KnowledgeDocument::getUpdatedAt);

        Page<KnowledgeDocument> page = documentMapper.selectPage(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<Long> ids = page.getRecords().stream().map(KnowledgeDocument::getId).toList();
        Map<Long, long[]> counts = aggregateChunkCounts(ids);

        return PageResponse.from(page, doc -> toListItem(doc, counts.getOrDefault(doc.getId(), new long[3])));
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentDetail getDetail(Long id) {
        KnowledgeDocument doc = requireDocument(id);
        long[] counts = aggregateChunkCounts(List.of(id)).getOrDefault(id, new long[3]);
        return new KnowledgeDocumentDetail(
                doc.getId(), doc.getDocCode(), doc.getTitle(),
                doc.getModule(), moduleLabel(doc.getModule()),
                doc.getSourceType(), sourceTypeLabel(doc.getSourceType()),
                doc.getBizType(), doc.getOperationType(), doc.getEntityName(), doc.getScenario(),
                splitKeywords(doc.getKeywords()),
                doc.getVersion(), doc.getContentFormat(), doc.getChunkStrategy(), doc.getStatus(),
                doc.getContent(), counts[0], counts[1], counts[2],
                doc.getCreatedAt(), doc.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public PageResponse<KnowledgeChunkView> pageChunks(Long docId, KnowledgeChunkQuery query) {
        requireDocument(docId);
        Page<KnowledgeChunk> page = chunkMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(KnowledgeChunk.class)
                        .eq(KnowledgeChunk::getDocId, docId)
                        .eq(StringUtils.hasText(query.getStatus()), KnowledgeChunk::getStatus, query.getStatus())
                        .eq(StringUtils.hasText(query.getVectorStatus()), KnowledgeChunk::getVectorStatus, query.getVectorStatus())
                        .orderByAsc(KnowledgeChunk::getSortOrder));
        return PageResponse.from(page, this::toChunkView);
    }

    // ---- documents: write ---------------------------------------------------------------------------

    @Transactional
    public KnowledgeSaveResult create(KnowledgeDocumentSaveRequest request) {
        validate(request);

        KnowledgeDocument doc = new KnowledgeDocument();
        applyRequest(doc, request);
        if (!StringUtils.hasText(doc.getDocCode())) {
            doc.setDocCode(generateDocCode(doc.getModule(), doc.getSourceType()));
        }
        doc.setContentHash(sha256(doc.getContent()));
        Long userId = currentUserId();
        doc.setCreatedBy(userId);
        doc.setUpdatedBy(userId);
        ensureDocCodeUnique(doc.getDocCode(), null);
        documentMapper.insert(doc);

        List<KnowledgeChunk> chunks = rechunk(doc);
        int[] result = maybeVectorize(doc, chunks, autoVectorize(request), false);
        boolean vectorized = vectorizer.isAvailable() && autoVectorize(request) && result[1] == 0 && result[0] > 0;

        return new KnowledgeSaveResult(doc.getId(), doc.getDocCode(), chunks.size(), vectorized);
    }

    @Transactional
    public KnowledgeSaveResult update(Long id, KnowledgeDocumentSaveRequest request) {
        validate(request);
        KnowledgeDocument doc = requireDocument(id);

        String newHash = sha256(request.content());
        boolean contentOrStrategyChanged = !Objects.equals(newHash, doc.getContentHash())
                || !Objects.equals(normalizeStrategy(request.chunkStrategy()), doc.getChunkStrategy());

        String newDocCode = StringUtils.hasText(request.docCode()) ? request.docCode().strip() : doc.getDocCode();
        ensureDocCodeUnique(newDocCode, id);

        applyRequest(doc, request);
        doc.setDocCode(newDocCode);
        doc.setContentHash(newHash);
        doc.setUpdatedBy(currentUserId());
        documentMapper.updateById(doc);

        List<KnowledgeChunk> chunks;
        int[] result;
        if (contentOrStrategyChanged) {
            deleteChunksAndVectors(doc.getId());
            chunks = rechunk(doc);
            result = maybeVectorize(doc, chunks, autoVectorize(request), false);
        } else {
            chunks = syncChunkMetadata(doc);
            if (autoVectorize(request)) {
                result = maybeVectorize(doc, chunks, true, true);
            } else {
                markStale(chunks);
                result = new int[]{0, 0};
            }
        }

        boolean vectorized = vectorizer.isAvailable() && autoVectorize(request) && result[1] == 0 && result[0] > 0;
        return new KnowledgeSaveResult(doc.getId(), doc.getDocCode(), chunks.size(), vectorized);
    }

    @Transactional
    public KnowledgeDocumentDetail changeStatus(Long id, String status) {
        if (!StringUtils.hasText(status) || !DOCUMENT_STATUSES.contains(status)) {
            throw new BusinessException("非法的文档状态：" + status);
        }
        KnowledgeDocument doc = requireDocument(id);
        doc.setStatus(status);
        doc.setUpdatedBy(currentUserId());
        documentMapper.updateById(doc);

        // Chunk status mirrors the document: only ENABLED docs contribute ENABLED (searchable) chunks.
        String chunkStatus = KnowledgeDocument.STATUS_ENABLED.equals(status)
                ? KnowledgeChunk.STATUS_ENABLED : KnowledgeChunk.STATUS_DISABLED;
        KnowledgeChunk patch = new KnowledgeChunk();
        patch.setStatus(chunkStatus);
        chunkMapper.update(patch, Wrappers.lambdaUpdate(KnowledgeChunk.class).eq(KnowledgeChunk::getDocId, id));
        // Search enforces document/chunk status live against MySQL, so no pgvector rewrite is needed here.
        return getDetail(id);
    }

    @Transactional
    public KnowledgeVectorizeResult vectorize(Long id, boolean force) {
        KnowledgeDocument doc = requireDocument(id);
        List<KnowledgeChunk> chunks;

        if (force) {
            deleteChunksAndVectors(doc.getId());
            doc.setContentHash(sha256(doc.getContent()));
            documentMapper.updateById(doc);
            chunks = rechunk(doc);
        } else {
            chunks = chunksOf(id);
            if (chunks.isEmpty()) {
                chunks = rechunk(doc);
            } else if (allIndexed(chunks)) {
                return new KnowledgeVectorizeResult(0, 0, chunks.size(), "内容未变化且全部分片已索引，已跳过");
            }
            vectorizer.deleteByDocument(id);
        }

        int[] result = embedChunks(doc, chunks);
        String message = vectorizer.isAvailable()
                ? String.format("向量化完成：成功 %d，失败 %d", result[0], result[1])
                : "AI 向量库未启用，已将分片标记为 FAILED（保留状态，不阻塞知识管理）";
        return new KnowledgeVectorizeResult(result[0], result[1], 0, message);
    }

    // ---- search -------------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public KnowledgeSearchResponse search(KnowledgeSearchRequest request) {
        if (request == null || !StringUtils.hasText(request.query())) {
            throw new BusinessException("检索内容 query 不能为空");
        }
        int topK = request.topK() == null ? SEARCH_DEFAULT_TOP_K : Math.min(Math.max(request.topK(), 1), SEARCH_MAX_TOP_K);
        int overFetch = Math.min(Math.max(topK * 3, topK), 50);

        // Throws a clear BusinessException when the vector link is unavailable — never a fake success.
        List<KnowledgeVectorizer.ScoredChunk> hits = vectorizer.search(request.query(), overFetch);
        if (hits.isEmpty()) {
            return new KnowledgeSearchResponse(request.query(), List.of());
        }

        List<Long> chunkIds = hits.stream().map(KnowledgeVectorizer.ScoredChunk::chunkId).distinct().toList();
        Map<Long, KnowledgeChunk> chunkById = chunkMapper.selectBatchIds(chunkIds).stream()
                .collect(Collectors.toMap(KnowledgeChunk::getId, c -> c));
        Map<Long, KnowledgeDocument> docById = loadDocuments(chunkById.values().stream()
                .map(KnowledgeChunk::getDocId).distinct().toList());

        List<KnowledgeSearchHit> records = new ArrayList<>();
        for (KnowledgeVectorizer.ScoredChunk hit : hits) {
            KnowledgeChunk chunk = chunkById.get(hit.chunkId());
            if (chunk == null || !KnowledgeChunk.STATUS_ENABLED.equals(chunk.getStatus())) {
                continue;
            }
            KnowledgeDocument doc = docById.get(chunk.getDocId());
            if (doc == null || !KnowledgeDocument.STATUS_ENABLED.equals(doc.getStatus())) {
                continue;
            }
            if (StringUtils.hasText(request.module()) && !request.module().equals(chunk.getModule())) {
                continue;
            }
            if (StringUtils.hasText(request.operationType()) && !request.operationType().equals(chunk.getOperationType())) {
                continue;
            }
            records.add(new KnowledgeSearchHit(chunk.getId(), chunk.getDocId(), chunk.getTitle(),
                    chunk.getModule(), chunk.getSourceType(), chunk.getOperationType(), hit.score(), chunk.getContent()));
            if (records.size() >= topK) {
                break;
            }
        }
        return new KnowledgeSearchResponse(request.query(), records);
    }

    // ---- chunking / vectorization helpers -----------------------------------------------------------

    private List<KnowledgeChunk> rechunk(KnowledgeDocument doc) {
        List<ChunkPiece> pieces = chunker.chunk(doc.getContent(), doc.getChunkStrategy(), doc.getTitle());
        List<KnowledgeChunk> chunks = new ArrayList<>();
        int order = 1;
        for (ChunkPiece piece : pieces) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocId(doc.getId());
            chunk.setChunkCode(String.format("%s_%03d", doc.getDocCode(), order));
            chunk.setTitle(truncate(piece.title(), 255));
            chunk.setModule(doc.getModule());
            chunk.setSourceType(doc.getSourceType());
            chunk.setBizType(doc.getBizType());
            chunk.setOperationType(doc.getOperationType());
            chunk.setEntityName(doc.getEntityName());
            chunk.setScenario(doc.getScenario());
            chunk.setKeywords(doc.getKeywords());
            chunk.setContent(piece.content());
            chunk.setContentHash(sha256(piece.content()));
            chunk.setSortOrder(order);
            chunk.setStatus(KnowledgeDocument.STATUS_ENABLED.equals(doc.getStatus())
                    ? KnowledgeChunk.STATUS_ENABLED : KnowledgeChunk.STATUS_DISABLED);
            chunk.setVectorStatus(KnowledgeChunk.VECTOR_PENDING);
            chunkMapper.insert(chunk);
            chunks.add(chunk);
            order++;
        }
        return chunks;
    }

    /** Metadata-only edit: push the document's inherited metadata down onto its existing chunks. */
    private List<KnowledgeChunk> syncChunkMetadata(KnowledgeDocument doc) {
        List<KnowledgeChunk> chunks = chunksOf(doc.getId());
        for (KnowledgeChunk chunk : chunks) {
            chunk.setModule(doc.getModule());
            chunk.setSourceType(doc.getSourceType());
            chunk.setBizType(doc.getBizType());
            chunk.setOperationType(doc.getOperationType());
            chunk.setEntityName(doc.getEntityName());
            chunk.setScenario(doc.getScenario());
            chunk.setKeywords(doc.getKeywords());
            chunkMapper.updateById(chunk);
        }
        return chunks;
    }

    private void deleteChunksAndVectors(Long docId) {
        chunkMapper.delete(Wrappers.lambdaQuery(KnowledgeChunk.class).eq(KnowledgeChunk::getDocId, docId));
        vectorizer.deleteByDocument(docId);
    }

    private int[] maybeVectorize(KnowledgeDocument doc, List<KnowledgeChunk> chunks, boolean autoVectorize, boolean deleteFirst) {
        if (!autoVectorize) {
            return new int[]{0, 0};
        }
        if (deleteFirst) {
            vectorizer.deleteByDocument(doc.getId());
        }
        return embedChunks(doc, chunks);
    }

    /** Embeds every chunk. Returns {@code [successCount, failedCount]}; unavailable link → all FAILED. */
    private int[] embedChunks(KnowledgeDocument doc, List<KnowledgeChunk> chunks) {
        int success = 0;
        int failed = 0;
        boolean available = vectorizer.isAvailable();
        for (KnowledgeChunk chunk : chunks) {
            if (!available) {
                setVectorStatus(chunk, KnowledgeChunk.VECTOR_FAILED, "AI 向量库未启用（未激活 ai profile 或未连接 pgvector）");
                failed++;
                continue;
            }
            try {
                vectorizer.index(doc, chunk);
                setVectorStatus(chunk, KnowledgeChunk.VECTOR_INDEXED, null);
                success++;
            } catch (RuntimeException ex) {
                setVectorStatus(chunk, KnowledgeChunk.VECTOR_FAILED, truncate(ex.getMessage(), 1000));
                failed++;
            }
        }
        return new int[]{success, failed};
    }

    private void markStale(List<KnowledgeChunk> chunks) {
        for (KnowledgeChunk chunk : chunks) {
            setVectorStatus(chunk, KnowledgeChunk.VECTOR_STALE, "元数据已更新，向量索引待刷新");
        }
    }

    private void setVectorStatus(KnowledgeChunk chunk, String status, String error) {
        chunk.setVectorStatus(status);
        chunk.setVectorErrorMessage(error);
        chunkMapper.updateById(chunk);
    }

    // ---- small helpers ------------------------------------------------------------------------------

    private void applyRequest(KnowledgeDocument doc, KnowledgeDocumentSaveRequest request) {
        doc.setTitle(request.title().strip());
        doc.setModule(request.module().strip());
        doc.setSourceType(request.sourceType().strip());
        doc.setBizType(blankToNull(request.bizType()));
        doc.setOperationType(blankToNull(request.operationType()));
        doc.setEntityName(blankToNull(request.entityName()));
        doc.setScenario(blankToNull(request.scenario()));
        doc.setKeywords(joinKeywords(request.keywords()));
        doc.setVersion(StringUtils.hasText(request.version()) ? request.version().strip() : "v1.0");
        doc.setContentFormat(normalizeFormat(request.contentFormat()));
        doc.setChunkStrategy(normalizeStrategy(request.chunkStrategy()));
        doc.setStatus(StringUtils.hasText(request.status()) && DOCUMENT_STATUSES.contains(request.status())
                ? request.status() : KnowledgeDocument.STATUS_ENABLED);
        doc.setContent(request.content());
        if (StringUtils.hasText(request.docCode())) {
            doc.setDocCode(request.docCode().strip());
        }
    }

    private void validate(KnowledgeDocumentSaveRequest request) {
        if (request == null) {
            throw new BusinessException("请求体不能为空");
        }
        if (!StringUtils.hasText(request.title())) {
            throw new BusinessException("知识标题 title 不能为空");
        }
        if (!StringUtils.hasText(request.module())) {
            throw new BusinessException("业务模块 module 不能为空");
        }
        if (!StringUtils.hasText(request.sourceType())) {
            throw new BusinessException("来源类型 sourceType 不能为空");
        }
        if (!StringUtils.hasText(request.content())) {
            throw new BusinessException("知识正文 content 不能为空");
        }
    }

    private boolean autoVectorize(KnowledgeDocumentSaveRequest request) {
        return Boolean.TRUE.equals(request.autoVectorize());
    }

    private KnowledgeDocument requireDocument(Long id) {
        KnowledgeDocument doc = id == null ? null : documentMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException("知识文档不存在");
        }
        return doc;
    }

    private void ensureDocCodeUnique(String docCode, Long excludeId) {
        Long count = documentMapper.selectCount(Wrappers.lambdaQuery(KnowledgeDocument.class)
                .eq(KnowledgeDocument::getDocCode, docCode)
                .ne(excludeId != null, KnowledgeDocument::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("知识编码 docCode 已存在：" + docCode);
        }
    }

    private List<KnowledgeChunk> chunksOf(Long docId) {
        return chunkMapper.selectList(Wrappers.lambdaQuery(KnowledgeChunk.class)
                .eq(KnowledgeChunk::getDocId, docId)
                .orderByAsc(KnowledgeChunk::getSortOrder));
    }

    private boolean allIndexed(List<KnowledgeChunk> chunks) {
        return !chunks.isEmpty() && chunks.stream().allMatch(c -> KnowledgeChunk.VECTOR_INDEXED.equals(c.getVectorStatus()));
    }

    private List<Long> docIdsByChunkVectorStatus(String vectorStatus) {
        return chunkMapper.selectList(Wrappers.lambdaQuery(KnowledgeChunk.class)
                        .select(KnowledgeChunk::getDocId)
                        .eq(KnowledgeChunk::getVectorStatus, vectorStatus)
                        .groupBy(KnowledgeChunk::getDocId)).stream()
                .map(KnowledgeChunk::getDocId)
                .distinct()
                .toList();
    }

    private Map<Long, KnowledgeDocument> loadDocuments(List<Long> docIds) {
        if (docIds.isEmpty()) {
            return Map.of();
        }
        return documentMapper.selectBatchIds(docIds).stream()
                .collect(Collectors.toMap(KnowledgeDocument::getId, d -> d));
    }

    /** doc_id -> [total, indexed, failed] chunk counts, in a single grouped query. */
    private Map<Long, long[]> aggregateChunkCounts(List<Long> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<KnowledgeChunk> wrapper = new QueryWrapper<KnowledgeChunk>()
                .select("doc_id AS docId",
                        "COUNT(*) AS total",
                        "SUM(CASE WHEN vector_status = 'INDEXED' THEN 1 ELSE 0 END) AS indexed",
                        "SUM(CASE WHEN vector_status = 'FAILED' THEN 1 ELSE 0 END) AS failed")
                .in("doc_id", docIds)
                .groupBy("doc_id");
        Map<Long, long[]> result = new HashMap<>();
        for (Map<String, Object> row : chunkMapper.selectMaps(wrapper)) {
            Long docId = toLong(row.get("docId"));
            if (docId != null) {
                result.put(docId, new long[]{toLong(row.get("total")), toLong(row.get("indexed")), toLong(row.get("failed"))});
            }
        }
        return result;
    }

    private KnowledgeDocumentListItem toListItem(KnowledgeDocument doc, long[] counts) {
        return new KnowledgeDocumentListItem(
                doc.getId(), doc.getDocCode(), doc.getTitle(),
                doc.getModule(), moduleLabel(doc.getModule()),
                doc.getSourceType(), sourceTypeLabel(doc.getSourceType()),
                doc.getContentFormat(), doc.getChunkStrategy(), doc.getVersion(), doc.getStatus(),
                counts[0], counts[1], counts[2], doc.getUpdatedAt());
    }

    private KnowledgeChunkView toChunkView(KnowledgeChunk chunk) {
        return new KnowledgeChunkView(chunk.getId(), chunk.getChunkCode(), chunk.getTitle(),
                chunk.getModule(), chunk.getSourceType(), chunk.getOperationType(), chunk.getContent(),
                chunk.getContentHash(), chunk.getSortOrder(), chunk.getStatus(),
                chunk.getVectorStatus(), chunk.getVectorErrorMessage());
    }

    private String moduleLabel(String module) {
        return module == null ? null : sysDictService.getLabel(MODULE_DICT, module);
    }

    private String sourceTypeLabel(String sourceType) {
        return sourceType == null ? null : sysDictService.getLabel(SOURCE_TYPE_DICT, sourceType);
    }

    private String joinKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }
        String joined = keywords.stream()
                .filter(StringUtils::hasText)
                .map(String::strip)
                .collect(Collectors.joining(","));
        return StringUtils.hasText(joined) ? joined : null;
    }

    private List<String> splitKeywords(String keywords) {
        if (!StringUtils.hasText(keywords)) {
            return List.of();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::strip)
                .filter(StringUtils::hasText)
                .toList();
    }

    private String normalizeStrategy(String strategy) {
        return StringUtils.hasText(strategy) && CHUNK_STRATEGIES.contains(strategy)
                ? strategy : MarkdownSemanticChunker.STRATEGY_SEMANTIC_MARKDOWN;
    }

    private String normalizeFormat(String format) {
        return StringUtils.hasText(format) && CONTENT_FORMATS.contains(format) ? format : "MARKDOWN";
    }

    private String generateDocCode(String module, String sourceType) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return (module + "_" + sourceType + "_" + random).toUpperCase();
    }

    private Long currentUserId() {
        CurrentUser user = CurrentUserContext.get();
        return user != null ? user.userId() : null;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.strip() : null;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}

-- V5__alter_knowledge_document_add_content.sql
-- P0 AI knowledge base: knowledge_document (created in V4) stores document metadata only. The P0 design
-- keeps the ORIGINAL body on the document and the split bodies on knowledge_chunk, so the document needs
-- a content column plus the metadata that chunks inherit (biz_type / operation_type / entity_name /
-- scenario / keywords). knowledge_chunk already has every required column (see V4), so it is untouched.
-- New migration on purpose: V4 is already applied and must never be edited.

ALTER TABLE knowledge_document
  ADD COLUMN content        LONGTEXT     NULL COMMENT '原始正文（Markdown/纯文本），分片后写入 knowledge_chunk' AFTER content_hash,
  ADD COLUMN biz_type       VARCHAR(80)  NULL COMMENT '业务类型，chunk 继承' AFTER content,
  ADD COLUMN operation_type VARCHAR(80)  NULL COMMENT '操作类型，chunk 继承' AFTER biz_type,
  ADD COLUMN entity_name    VARCHAR(100) NULL COMMENT '关联实体名，chunk 继承' AFTER operation_type,
  ADD COLUMN scenario       VARCHAR(255) NULL COMMENT '业务场景描述，chunk 继承' AFTER entity_name,
  ADD COLUMN keywords       VARCHAR(500) NULL COMMENT '关键词（逗号分隔），chunk 继承' AFTER scenario;

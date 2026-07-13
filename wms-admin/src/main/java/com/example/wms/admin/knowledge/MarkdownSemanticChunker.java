package com.example.wms.admin.knowledge;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * P0 knowledge chunker. Splitting is a BACKEND responsibility (never the frontend). Three strategies:
 *
 * <ul>
 *   <li>{@code SEMANTIC_MARKDOWN} — split on Markdown H2/H3 headings, keep the parent heading as the
 *       chunk title; sections longer than {@link #MAX_CHARS} are further split by paragraph, and
 *       consecutive too-short sections are merged.</li>
 *   <li>{@code FIXED_SIZE} — fixed-length windows ({@link #FIXED_SIZE_CHARS}) with
 *       {@link #FIXED_SIZE_OVERLAP} overlap.</li>
 *   <li>{@code NONE} — the whole body as a single chunk (short rules / field dictionaries).</li>
 * </ul>
 *
 * Sizes are measured in characters, which for CJK text is effectively "中文字" count.
 */
@Component
public class MarkdownSemanticChunker {

    static final int MIN_CHARS = 300;
    static final int TARGET_CHARS = 800;
    static final int MAX_CHARS = 1200;
    static final int FIXED_SIZE_CHARS = 800;
    static final int FIXED_SIZE_OVERLAP = 80;

    public static final String STRATEGY_SEMANTIC_MARKDOWN = "SEMANTIC_MARKDOWN";
    public static final String STRATEGY_FIXED_SIZE = "FIXED_SIZE";
    public static final String STRATEGY_NONE = "NONE";

    /** One produced chunk: a display title (parent heading / doc title) and its body text. */
    public record ChunkPiece(String title, String content) {
    }

    public List<ChunkPiece> chunk(String content, String strategy, String docTitle) {
        String body = content == null ? "" : content.strip();
        String fallbackTitle = StringUtils.hasText(docTitle) ? docTitle.strip() : "知识分片";
        if (body.isEmpty()) {
            return List.of();
        }

        String normalized = strategy == null ? STRATEGY_SEMANTIC_MARKDOWN : strategy;
        return switch (normalized) {
            case STRATEGY_NONE -> List.of(new ChunkPiece(fallbackTitle, body));
            case STRATEGY_FIXED_SIZE -> fixedSize(body, fallbackTitle);
            default -> semanticMarkdown(body, fallbackTitle);
        };
    }

    // ---- FIXED_SIZE ---------------------------------------------------------------------------------

    private List<ChunkPiece> fixedSize(String body, String docTitle) {
        List<ChunkPiece> pieces = new ArrayList<>();
        int step = Math.max(1, FIXED_SIZE_CHARS - FIXED_SIZE_OVERLAP);
        int index = 1;
        for (int start = 0; start < body.length(); start += step) {
            int end = Math.min(body.length(), start + FIXED_SIZE_CHARS);
            String slice = body.substring(start, end).strip();
            if (!slice.isEmpty()) {
                pieces.add(new ChunkPiece(docTitle + " #" + index++, slice));
            }
            if (end >= body.length()) {
                break;
            }
        }
        return pieces.isEmpty() ? List.of(new ChunkPiece(docTitle, body)) : pieces;
    }

    // ---- SEMANTIC_MARKDOWN --------------------------------------------------------------------------

    private record Section(String title, String text) {
    }

    private List<ChunkPiece> semanticMarkdown(String body, String docTitle) {
        List<Section> sections = splitByHeadings(body, docTitle);

        // Expand oversize sections by paragraph packing.
        List<ChunkPiece> expanded = new ArrayList<>();
        for (Section section : sections) {
            if (section.text().length() <= MAX_CHARS) {
                expanded.add(new ChunkPiece(section.title(), section.text()));
            } else {
                expanded.addAll(packParagraphs(section.title(), section.text()));
            }
        }

        // Merge consecutive too-short pieces so tiny fragments don't become standalone chunks.
        List<ChunkPiece> merged = new ArrayList<>();
        for (ChunkPiece piece : expanded) {
            if (!merged.isEmpty()) {
                ChunkPiece prev = merged.get(merged.size() - 1);
                boolean prevTooShort = prev.content().length() < MIN_CHARS;
                boolean combinedFits = prev.content().length() + piece.content().length() <= TARGET_CHARS;
                if (prevTooShort && combinedFits) {
                    merged.set(merged.size() - 1, new ChunkPiece(prev.title(), prev.content() + "\n\n" + piece.content()));
                    continue;
                }
            }
            merged.add(piece);
        }
        return merged.isEmpty() ? List.of(new ChunkPiece(docTitle, body)) : merged;
    }

    private List<Section> splitByHeadings(String body, String docTitle) {
        String[] lines = body.split("\n", -1);
        List<Section> sections = new ArrayList<>();

        String parentH2 = null;
        String currentTitle = docTitle;
        StringBuilder buffer = new StringBuilder();

        for (String line : lines) {
            String heading = headingText(line, 2);
            String subHeading = headingText(line, 3);
            if (heading != null || subHeading != null) {
                flushSection(sections, currentTitle, buffer);
                if (heading != null) {
                    parentH2 = heading;
                    currentTitle = heading;
                } else {
                    currentTitle = parentH2 == null ? subHeading : parentH2 + " / " + subHeading;
                }
                buffer.append(line).append('\n');
            } else {
                buffer.append(line).append('\n');
            }
        }
        flushSection(sections, currentTitle, buffer);

        if (sections.isEmpty()) {
            sections.add(new Section(docTitle, body));
        }
        return sections;
    }

    private void flushSection(List<Section> sections, String title, StringBuilder buffer) {
        String text = buffer.toString().strip();
        buffer.setLength(0);
        if (!text.isEmpty()) {
            sections.add(new Section(title, text));
        }
    }

    /** Returns the heading text if {@code line} is an ATX heading of exactly {@code level}, else null. */
    private String headingText(String line, int level) {
        String trimmed = line.strip();
        String prefix = "#".repeat(level) + " ";
        if (trimmed.startsWith(prefix) && !trimmed.startsWith("#".repeat(level + 1))) {
            return trimmed.substring(prefix.length()).strip();
        }
        return null;
    }

    private List<ChunkPiece> packParagraphs(String title, String text) {
        String[] paragraphs = text.split("\n\\s*\n");
        List<ChunkPiece> pieces = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String rawParagraph : paragraphs) {
            String paragraph = rawParagraph.strip();
            if (paragraph.isEmpty()) {
                continue;
            }
            if (paragraph.length() > MAX_CHARS) {
                // A single huge paragraph: hard-split it into MAX_CHARS windows.
                flushBuffer(pieces, title, current);
                for (int start = 0; start < paragraph.length(); start += MAX_CHARS) {
                    int end = Math.min(paragraph.length(), start + MAX_CHARS);
                    pieces.add(new ChunkPiece(title, paragraph.substring(start, end).strip()));
                }
                continue;
            }
            if (current.length() > 0 && current.length() + paragraph.length() + 2 > TARGET_CHARS) {
                flushBuffer(pieces, title, current);
            }
            if (current.length() > 0) {
                current.append("\n\n");
            }
            current.append(paragraph);
        }
        flushBuffer(pieces, title, current);
        return pieces;
    }

    private void flushBuffer(List<ChunkPiece> pieces, String title, StringBuilder current) {
        String text = current.toString().strip();
        current.setLength(0);
        if (!text.isEmpty()) {
            pieces.add(new ChunkPiece(title, text));
        }
    }
}

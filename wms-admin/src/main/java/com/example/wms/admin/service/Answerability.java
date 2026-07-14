package com.example.wms.admin.service;

/**
 * Whether a RAG question could be answered from retrieved knowledge. Only {@link #ANSWERABLE} and
 * {@link #NOT_FOUND} are ever produced by {@link AiRagAskService} today; {@link #OUT_OF_SCOPE} and
 * {@link #PARTIALLY_ANSWERABLE} are reserved for future refinement (e.g. distinguishing "no knowledge
 * at all" from "some relevant knowledge but not enough to fully answer").
 */
public enum Answerability {
    ANSWERABLE("可回答"),
    NOT_FOUND("知识库未命中"),
    OUT_OF_SCOPE("超出范围"),
    PARTIALLY_ANSWERABLE("部分可回答");

    private final String label;

    Answerability(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

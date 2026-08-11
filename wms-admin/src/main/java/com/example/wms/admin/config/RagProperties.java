package com.example.wms.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds {@code app.ai.rag.*}.
 *
 * <p>Corrected 2026-08-10: this used to say the values came from a local {@code application-ai.yml},
 * loaded only under an {@code ai} profile group. That file and that profile-group mechanism no longer
 * exist in this repo. The current override values (matching the field defaults below) instead come from
 * the Nacos config center (dataId {@code wms-service.yaml}, group {@code WMS_GROUP}), imported
 * unconditionally by {@code application.yml} regardless of active Spring profile — see
 * {@code PROJECT_CONTEXT.md} §11. Whether the {@code test} profile still avoids these Nacos-sourced
 * values some other way (e.g. via {@code wms-admin/src/test/resources/application-test.yml}) was not
 * re-verified when this comment was corrected; either way, the field defaults below remain the fallback
 * if no override binds.
 */
@Component
@ConfigurationProperties(prefix = "app.ai.rag")
public class RagProperties {

    /** Default number of candidate knowledge chunks to retrieve when a request doesn't override it. */
    private int topK = 3;

    /** Minimum similarityScore (1 - cosine distance) a hit must reach to be usable; see AiRagAskService. */
    private double similarityThreshold = 0.65;

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }
}

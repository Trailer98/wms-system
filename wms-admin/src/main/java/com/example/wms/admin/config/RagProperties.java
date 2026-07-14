package com.example.wms.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds {@code app.ai.rag.*} (see application-ai.yml). Only loaded under the {@code ai} profile group,
 * so the field defaults below (matching the YAML values) are what's actually in effect under every
 * other profile (e.g. {@code test}) where that YAML file never loads — the properties bean still binds
 * successfully with these defaults rather than resolving to 0/0.0.
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

package com.studyassistant.util;

import com.studyassistant.model.DocumentChunk;
import java.util.*;

/**
 * VectorUtils - Utility class for computing cosine similarity
 * between embedding vectors and retrieving the most similar chunks.
 */
public class VectorUtils {

    /**
     * Compute cosine similarity between two vectors.
     * Returns a value in [-1, 1], where 1 = identical direction, 0 = orthogonal.
     *
     * @param a first vector
     * @param b second vector
     * @return cosine similarity score
     */
    public static double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Vectors must not be null");
        }
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }

        double dot = 0.0;
        double magA = 0.0;
        double magB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            magA += a[i] * a[i];
            magB += b[i] * b[i];
        }

        if (magA == 0 || magB == 0) {
            // avoid division by zero
            return 0.0;
        }

        return dot / (Math.sqrt(magA) * Math.sqrt(magB));
    }

    /**
     * Find the top-K most similar chunks to a given query vector.
     *
     * @param queryVector embedding for the query text
     * @param chunks      list of DocumentChunk objects (with embedding strings)
     * @param k           number of results to return
     * @return top K chunks with highest similarity scores
     */
    public static List<ScoredChunk> findTopSimilar(float[] queryVector,
            List<DocumentChunk> chunks,
            int k) {
        List<ScoredChunk> scored = new ArrayList<>();

        for (DocumentChunk chunk : chunks) {
            float[] emb = parseEmbedding(chunk.getEmbedding());
            if (emb == null || emb.length != queryVector.length) {
                // skip invalid or mismatched embeddings
                continue;
            }
            double score = cosineSimilarity(queryVector, emb);
            scored.add(new ScoredChunk(chunk, score));
        }

        // sort descending by score
        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // return top K
        return scored.subList(0, Math.min(k, scored.size()));
    }

    /**
     * Parse a comma-separated string into a float array.
     */
    public static float[] parseEmbedding(String s) {
        if (s == null || s.isBlank())
            return null;
        String[] parts = s.split(",");
        float[] v = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                v[i] = Float.parseFloat(parts[i]);
            } catch (NumberFormatException e) {
                return null; // skip malformed vectors
            }
        }
        return v;
    }

    /**
     * Holder for a chunk and its similarity score.
     */
    public static class ScoredChunk {
        public final DocumentChunk chunk;
        public final double score;

        public ScoredChunk(DocumentChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}

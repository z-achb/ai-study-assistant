package com.studyassistant.service;

import com.studyassistant.dto.QueryRequest;
import com.studyassistant.dto.QueryResponse;
import com.studyassistant.dto.QueryResponse.SourceChunk;
import com.studyassistant.model.DocumentChunk;
import com.studyassistant.repository.DocumentChunkRepository;
import com.studyassistant.util.VectorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * QueryService
 *
 * Handles user queries against stored document chunks using embeddings.
 * Steps:
 *  1. Generate an embedding for the query.
 *  2. Compare query vector to all chunk embeddings via cosine similarity.
 *  3. Select the top K most similar chunks.
 *  4. Send context + question to the chat model to produce an answer.
 */
@Service
public class QueryService {

    @Autowired
    private OpenAIService openAIService;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    public QueryResponse query(QueryRequest request) {
        try {
            String question = request.getQuestion();
            System.out.println("Query: " + question);

            // Step 1: Generate embedding for question
            System.out.println("Generating question embedding...");
            float[] queryVector = openAIService.generateEmbedding(question);
            int expectedDim = queryVector.length;

            // Step 2: Retrieve all document chunks
            System.out.println("Fetching chunks from database...");
            List<DocumentChunk> allChunks = chunkRepository.findAll();
            System.out.println("Found " + allChunks.size() + " chunks.");

            // Step 3: Compute cosine similarity for each valid chunk
            List<ScoredChunk> scoredChunks = new ArrayList<>();

            for (DocumentChunk chunk : allChunks) {
                float[] chunkVector = parseEmbedding(chunk.getEmbedding());
                if (chunkVector == null || chunkVector.length != expectedDim) {
                    System.out.println("Skipping chunk ID " + chunk.getId() +
                            " (invalid or mismatched vector length: " +
                            (chunkVector == null ? "null" : chunkVector.length) + ")");
                    continue;
                }

                double score = VectorUtils.cosineSimilarity(queryVector, chunkVector);
                scoredChunks.add(new ScoredChunk(chunk, score));
            }

            if (scoredChunks.isEmpty()) {
                return new QueryResponse(
                        "No valid embeddings found. Try re-uploading your document.",
                        Collections.emptyList());
            }

            // Step 4: Sort and select top K
            scoredChunks.sort((a, b) -> Double.compare(b.score, a.score));
            int topK = Math.min(request.getTopK(), scoredChunks.size());
            List<ScoredChunk> topChunks = scoredChunks.subList(0, topK);

            // Step 5: Construct context
            String context = topChunks.stream()
                    .map(c -> c.chunk.getContent())
                    .collect(Collectors.joining("\n\n"));

            // Step 6: Generate answer using the chat model
            System.out.println("Sending top " + topK + " chunks to model...");
            String systemPrompt = "You are a helpful study assistant. Use the provided context to answer accurately.";
            String userPrompt = "Context:\n" + context + "\n\nQuestion: " + question;

            String answer = openAIService.generateChatCompletion(systemPrompt, userPrompt);

            // Step 7: Build structured source list (matches DTO)
            List<SourceChunk> sources = topChunks.stream()
                    .map(c -> new SourceChunk(
                            c.chunk.getId(),
                            c.chunk.getContent(),
                            c.chunk.getDocument().getFilename(),
                            c.chunk.getChunkIndex(),
                            c.score))
                    .collect(Collectors.toList());

            return new QueryResponse(answer, sources);

        } catch (IOException e) {
            e.printStackTrace();
            return new QueryResponse(
                    "Error processing query: " + e.getMessage(),
                    Collections.emptyList());
        }
    }

    private static float[] parseEmbedding(String s) {
        if (s == null || s.isBlank()) return null;
        String[] parts = s.split(",");
        float[] v = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                v[i] = Float.parseFloat(parts[i]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return v;
    }

    private static class ScoredChunk {
        final DocumentChunk chunk;
        final double score;

        ScoredChunk(DocumentChunk c, double s) {
            this.chunk = c;
            this.score = s;
        }
    }
}

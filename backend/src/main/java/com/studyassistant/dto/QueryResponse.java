package com.studyassistant.dto;

import java.util.List;

/**
 * QueryResponse - Response with answer and sources
 * 
 * Example JSON:
 * {
 *   "answer": "Photosynthesis is...",
 *   "sources": [
 *     {
 *       "chunkId": 42,
 *       "content": "Photosynthesis is the process...",
 *       "documentName": "biology_textbook.pdf",
 *       "chunkIndex": 5,
 *       "similarity": 0.91
 *     }
 *   ]
 * }
 */
public class QueryResponse {
    
    private String answer;
    private List<SourceChunk> sources;

    // Constructors
    public QueryResponse() {
    }
    public QueryResponse(String answer, List<SourceChunk> sources) {
        this.answer = answer;
        this.sources = sources;
    }

    // Getters and Setters
    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<SourceChunk> getSources() {
        return sources;
    }

    public void setSources(List<SourceChunk> sources) {
        this.sources = sources;
    }

    /**
     * Inner class for source chunks
     */
    public static class SourceChunk {
        private Long chunkId;
        private String content;
        private String documentName;
        private int chunkIndex;
        private double similarity;

        public SourceChunk() {
        }

        public SourceChunk(Long chunkId, String content, String documentName, 
                          int chunkIndex, double similarity) {
            this.chunkId = chunkId;
            this.content = content;
            this.documentName = documentName;
            this.chunkIndex = chunkIndex;
            this.similarity = similarity;
        }

        // Getters and Setters
        public Long getChunkId() {
            return chunkId;
        }

        public void setChunkId(Long chunkId) {
            this.chunkId = chunkId;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getDocumentName() {
            return documentName;
        }

        public void setDocumentName(String documentName) {
            this.documentName = documentName;
        }

        public int getChunkIndex() {
            return chunkIndex;
        }

        public void setChunkIndex(int chunkIndex) {
            this.chunkIndex = chunkIndex;
        }

        public double getSimilarity() {
            return similarity;
        }

        public void setSimilarity(double similarity) {
            this.similarity = similarity;
        }
    }
}
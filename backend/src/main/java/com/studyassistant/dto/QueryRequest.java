package com.studyassistant.dto;

/**
 * QueryRequest - Request body for asking questions
 * 
 * Example JSON:
 * {
 *   "question": "What is photosynthesis?",
 *   "topK": 5
 * }
 */
public class QueryRequest {
    
    private String question;
    private Integer topK = 5; // Default to 5 chunks

    // Constructors
    public QueryRequest() {
    }

    public QueryRequest(String question, Integer topK) {
        this.question = question;
        this.topK = topK;
    }

    // Getters and Setters
    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }
}
package com.studyassistant.dto;

/**
 * UploadResponse - Response after uploading a document
 * 
 * Example JSON:
 * {
 *   "message": "Document uploaded successfully",
 *   "documentId": 1,
 *   "filename": "biology_textbook.pdf",
 *   "chunkCount": 42,
 *   "success": true
 * }
 */
public class UploadResponse {
    
    private String message;
    private Long documentId;
    private String filename;
    private Integer chunkCount;
    private boolean success;

    // Constructors
    public UploadResponse() {
    }

    public UploadResponse(String message, Long documentId, String filename, 
                         Integer chunkCount, boolean success) {
        this.message = message;
        this.documentId = documentId;
        this.filename = filename;
        this.chunkCount = chunkCount;
        this.success = success;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
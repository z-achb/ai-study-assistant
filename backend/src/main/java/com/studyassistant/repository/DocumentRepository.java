package com.studyassistant.repository;

import com.studyassistant.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * DocumentRepository - Database access for Document entities
 * 
 * By extending JpaRepository, we automatically get these methods:
 * - save(document) - Insert or update
 * - findById(id) - Get document by ID
 * - findAll() - Get all documents
 * - deleteById(id) - Delete by ID
 * - count() - Count total documents
 * 
 * We can also define custom query methods!
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * Find document by exact filename
     * Spring automatically generates the SQL: 
     * SELECT * FROM documents WHERE filename = ?
     */
    Optional<Document> findByFilename(String filename);

    /**
     * Find all documents ordered by upload date (newest first)
     * SQL: SELECT * FROM documents ORDER BY uploaded_at DESC
     */
    List<Document> findAllByOrderByUploadedAtDesc();

    /**
     * Check if a document with this filename already exists
     * SQL: SELECT EXISTS(SELECT 1 FROM documents WHERE filename = ?)
     */
    boolean existsByFilename(String filename);

    /**
     * Custom query: Get total number of chunks across all documents
     * @Query lets us write custom JPQL (Java Persistence Query Language)
     */
    @Query("SELECT SUM(d.chunkCount) FROM Document d")
    Long getTotalChunkCount();

    /**
     * Get documents with more than X chunks
     * Useful for finding large documents
     */
    List<Document> findByChunkCountGreaterThan(Integer minChunks);
}
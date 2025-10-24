package com.studyassistant.repository;

import com.studyassistant.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * DocumentChunkRepository - Database access for DocumentChunk entities
 * 
 * This is where we'll fetch chunks for RAG retrieval
 */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /**
     * Find all chunks belonging to a specific document
     * Ordered by chunk index (maintains document order)
     * 
     * SQL: SELECT * FROM document_chunks 
     *      WHERE document_id = ? 
     *      ORDER BY chunk_index ASC
     */
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    /**
     * Find all chunks for a specific document (alternative method)
     */
    List<DocumentChunk> findByDocument_Id(Long documentId);

    /**
     * Get total number of chunks across all documents
     * We'll use this for statistics
     */
    @Query("SELECT COUNT(c) FROM DocumentChunk c")
    Long countAllChunks();

    /**
     * Find all chunks (we'll use this for similarity search)
     * Since we don't have pgvector, we'll fetch all chunks and 
     * compute similarity in Java
     * 
     * For large datasets, you'd want to add pagination or filtering
     */
    @Query("SELECT c FROM DocumentChunk c")
    List<DocumentChunk> findAllChunks();

    /**
     * Find chunks by document ID with custom query
     * This demonstrates JPQL with parameters
     */
    @Query("SELECT c FROM DocumentChunk c WHERE c.document.id = :documentId ORDER BY c.chunkIndex")
    List<DocumentChunk> findChunksByDocumentId(@Param("documentId") Long documentId);

    /**
     * Delete all chunks for a specific document
     * Useful when re-processing a document
     */
    void deleteByDocumentId(Long documentId);
}
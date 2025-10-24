package com.studyassistant.controller;

import com.studyassistant.dto.UploadResponse;
import com.studyassistant.model.Document;
import com.studyassistant.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * DocumentController - REST API for document management
 * 
 * Endpoints:
 * POST   /api/documents/upload - Upload a PDF
 * GET    /api/documents - Get all documents
 * GET    /api/documents/{id} - Get specific document
 * DELETE /api/documents/{id} - Delete document
 * GET    /api/documents/stats - Get statistics
 */
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*") // Allow requests from React frontend
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * Upload a PDF document
     * 
     * Example: POST /api/documents/upload
     * Body: multipart/form-data with "file" field
     * 
     * curl -X POST http://localhost:8080/api/documents/upload \
     *   -F "file=@textbook.pdf"
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new UploadResponse("File is empty", null, null, null, false)
                );
            }

            // Check if it's a PDF
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest().body(
                    new UploadResponse("Only PDF files are supported", null, null, null, false)
                );
            }

            // Process the document
            System.out.println("📥 Uploading: " + filename);
            Document document = documentService.processDocument(file);

            // Return success response
            UploadResponse response = new UploadResponse(
                "Document uploaded and processed successfully",
                document.getId(),
                document.getFilename(),
                document.getChunkCount(),
                true
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new UploadResponse("Error processing document: " + e.getMessage(), 
                                 null, null, null, false)
            );
        }
    }

    /**
     * Get all documents
     * 
     * Example: GET /api/documents
     */
    @GetMapping
    public ResponseEntity<List<Document>> getAllDocuments() {
        try {
            List<Document> documents = documentService.getAllDocuments();
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a specific document by ID
     * 
     * Example: GET /api/documents/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<Document> getDocument(@PathVariable Long id) {
        try {
            Document document = documentService.getDocumentById(id);
            return ResponseEntity.ok(document);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a document
     * 
     * Example: DELETE /api/documents/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDocument(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            return ResponseEntity.ok("Document deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting document");
        }
    }

    /**
     * Get statistics
     * 
     * Example: GET /api/documents/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<DocumentService.DocumentStats> getStats() {
        try {
            DocumentService.DocumentStats stats = documentService.getStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
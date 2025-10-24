package com.studyassistant.service;

import com.studyassistant.model.Document;
import com.studyassistant.model.DocumentChunk;
import com.studyassistant.repository.DocumentRepository;
import com.studyassistant.repository.DocumentChunkRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * DocumentService - Handles PDF processing, chunking, and storage
 * 
 * The RAG pipeline:
 * 1. Upload PDF
 * 2. Extract text using PDFBox
 * 3. Split text into chunks (with overlap)
 * 4. Generate embeddings for each chunk
 * 5. Store in database
 */
@Service
public class DocumentService {

    private static final int EMBED_BATCH_SIZE = 5; 
    private static final long BETWEEN_BATCH_MS = 2000;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository chunkRepository;

    @Autowired
    private OpenAIService openAIService;

    // Chunk settings from application.properties
    @Value("${app.chunk.size}")
    private int chunkSize;

    @Value("${app.chunk.overlap}")
    private int chunkOverlap;

    /**
     * Process and store a PDF document
     * 
     * @param file The uploaded PDF file
     * @return The saved Document entity
     */
    public Document processDocument(MultipartFile file) throws IOException {
        System.out.println("📄 Processing document: " + file.getOriginalFilename());

        // Step 1: Extract text from PDF
        String fullText = extractTextFromPdf(file);
        System.out.println("✓ Extracted " + fullText.length() + " characters");

        // Step 2: Create Document entity
        Document document = new Document(
            file.getOriginalFilename(),
            file.getSize(),
            countPages(file)
        );
        
        // Save document first (so we have an ID for foreign keys)
        document = documentRepository.save(document);
        System.out.println("✓ Saved document with ID: " + document.getId());

        // Step 3: Split text into chunks
        List<String> textChunks = chunkText(fullText);
        System.out.println("✓ Created " + textChunks.size() + " chunks");

        // Step 4: Generate embeddings in batches and create DocumentChunk entities
        List<DocumentChunk> chunks = new ArrayList<>();

        // A) Precompute offsets
        List<int[]> offsets = new ArrayList<>(textChunks.size());
        int startOffset = 0;
        for (int i = 0; i < textChunks.size(); i++) {
            int endOffset = startOffset + textChunks.get(i).length();
            offsets.add(new int[] { startOffset, endOffset });
            startOffset += (chunkSize - chunkOverlap);
        }

        // B) Batch calls
        try {
            for (int i = 0; i < textChunks.size(); i += EMBED_BATCH_SIZE) {
                int end = Math.min(i + EMBED_BATCH_SIZE, textChunks.size());
                List<String> batchTexts = textChunks.subList(i, end);

                System.out
                        .println("  Generating embeddings for chunks " + (i + 1) + "–" + end + "/" + textChunks.size());
                List<float[]> vectors = openAIService.generateEmbeddingsBatch(batchTexts);

                for (int j = 0; j < vectors.size(); j++) {
                    int idx = i + j;
                    int[] off = offsets.get(idx);
                    String embeddingString = openAIService.embeddingToString(vectors.get(j));
                    chunks.add(new DocumentChunk(textChunks.get(idx), embeddingString, idx, off[0], off[1], document));
                }

                try {
                    Thread.sleep(BETWEEN_BATCH_MS);
                } catch (InterruptedException ignored) {
                }
            }
        } catch (IOException e) {
            System.err.println("⚠️ Embedding repeatedly rate-limited: " + e.getMessage());
            // Fallback: save chunks with empty embeddings so upload completes
            for (int idx = chunks.size(); idx < textChunks.size(); idx++) {
                int[] off = offsets.get(idx);
                chunks.add(new DocumentChunk(textChunks.get(idx), "", idx, off[0], off[1], document));
            }
        }


        // Step 5: Save all chunks to database
        chunkRepository.saveAll(chunks);
        System.out.println("✓ Saved all chunks to database");

        // Step 6: Update document with chunk count
        document.setChunkCount(chunks.size());
        documentRepository.save(document);

        System.out.println("✅ Document processing complete!");
        return document;
    }

    /**
     * Extract text from PDF using Apache PDFBox
     * 
     * @param file The PDF file
     * @return Extracted text as a single string
     */
    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument pdfDocument = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdfDocument);
            
            // Clean up the text (remove excessive whitespace)
            text = text.replaceAll("\\s+", " ").trim();
            
            return text;
        }
    }

    /**
     * Count pages in PDF
     */
    private int countPages(MultipartFile file) throws IOException {
        try (PDDocument pdfDocument = PDDocument.load(file.getInputStream())) {
            return pdfDocument.getNumberOfPages();
        }
    }

    /**
     * Split text into overlapping chunks
     * 
     * Why overlap? Consider this text:
     * "...end of chunk 1. Important sentence. Start of chunk 2..."
     * 
     * Without overlap, "Important sentence" might get split awkwardly.
     * With overlap, it appears complete in at least one chunk!
     * 
     * @param text Full document text
     * @return List of text chunks
     */
    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        
        int textLength = text.length();
        int start = 0;

        while (start < textLength) {
            // Calculate end position
            int end = Math.min(start + chunkSize, textLength);

            // Try to break at sentence boundary (avoid splitting mid-sentence)
            if (end < textLength) {
                // Look for sentence ending punctuation
                int lastPeriod = text.lastIndexOf('.', end);
                int lastQuestion = text.lastIndexOf('?', end);
                int lastExclamation = text.lastIndexOf('!', end);
                
                int lastSentenceEnd = Math.max(lastPeriod, Math.max(lastQuestion, lastExclamation));
                
                // If we found a sentence boundary within reasonable range, use it
                if (lastSentenceEnd > start + (chunkSize / 2)) {
                    end = lastSentenceEnd + 1; // Include the punctuation
                }
            }

            // Extract chunk
            String chunk = text.substring(start, end).trim();
            
            // Only add non-empty chunks
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // Move to next chunk (with overlap)
            start += (chunkSize - chunkOverlap);
        }

        return chunks;
    }

    /**
     * Get all documents
     */
    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadedAtDesc();
    }

    /**
     * Get document by ID
     */
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
    }

    /**
     * Delete document and all its chunks
     */
    public void deleteDocument(Long id) {
        Document document = getDocumentById(id);
        documentRepository.delete(document); // Cascades to chunks
        System.out.println("🗑️ Deleted document: " + document.getFilename());
    }

    /**
     * Get statistics
     */
    public DocumentStats getStats() {
        long totalDocuments = documentRepository.count();
        Long totalChunks = documentRepository.getTotalChunkCount();
        
        return new DocumentStats(
            totalDocuments,
            totalChunks != null ? totalChunks : 0L
        );
    }

    /**
     * Simple stats class
     */
    public static class DocumentStats {
        public long totalDocuments;
        public long totalChunks;

        public DocumentStats(long totalDocuments, long totalChunks) {
            this.totalDocuments = totalDocuments;
            this.totalChunks = totalChunks;
        }
    }
}
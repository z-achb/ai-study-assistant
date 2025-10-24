package com.studyassistant.controller;

import com.studyassistant.dto.QueryRequest;
import com.studyassistant.dto.QueryResponse;
import com.studyassistant.service.QueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * QueryController
 *
 * Handles endpoints for querying uploaded documents.
 */
@RestController
@RequestMapping("/api/query")
@CrossOrigin
public class QueryController {

    @Autowired
    private QueryService queryService;

    /**
     * POST /api/query
     *
     * Request body example:
     * {
     *   "question": "What is in this document?",
     *   "topK": 3
     * }
     *
     * Response example:
     * {
     *   "answer": "This document discusses ...",
     *   "sources": [ ... ]
     * }
     */
    @PostMapping
    public ResponseEntity<QueryResponse> queryDocument(@RequestBody QueryRequest request) {
        QueryResponse response = queryService.query(request);
        return ResponseEntity.ok(response);
    }
}

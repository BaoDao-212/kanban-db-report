package com.example.graph.controller;

import com.example.graph.dto.PerformanceResult;
import com.example.graph.service.GraphService;
import com.example.graph.service.GraphService.RelationshipBatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api/bulk")
@RequiredArgsConstructor
@Slf4j
public class BulkInsertController {

    private final GraphService graphService;
    
    private static final int BATCH_SIZE = 1000;

    @PostMapping("/insert-large-dataset")
    public ResponseEntity<PerformanceResult> insertLargeDataset(
            @RequestParam(defaultValue = "50000") int nodeCount,
            @RequestParam(defaultValue = "200000") int relationshipCount) {
        
        long overallStartTime = System.currentTimeMillis();
        
        log.info("Starting bulk insert: {} nodes, {} relationships", nodeCount, relationshipCount);
        
        try {
            // Clear existing data
            log.info("Clearing existing data...");
            graphService.deleteAllNodes();
            
            // Phase 1: Create nodes in batches
            log.info("Creating {} nodes in batches of {}...", nodeCount, BATCH_SIZE);
            List<String> allNodeIds = createNodesBatched(nodeCount);
            
            // Force garbage collection between phases
            System.gc();
            
            // Phase 2: Create relationships in batches
            log.info("Creating {} relationships in batches of {}...", relationshipCount, BATCH_SIZE);
            createRelationshipsBatched(allNodeIds, relationshipCount);
            
            long overallEndTime = System.currentTimeMillis();
            long totalDuration = overallEndTime - overallStartTime;
            
            // Verify counts
            long finalNodeCount = graphService.countNodes();
            long finalRelCount = graphService.countRelationships();
            
            PerformanceResult result = PerformanceResult.builder()
                    .database(graphService.getDatabaseType())
                    .operation("BULK_INSERT_LARGE_DATASET")
                    .executionTimeMs(totalDuration)
                    .recordCount(nodeCount + relationshipCount)
                    .additionalInfo(String.format(
                            "Created %d nodes (%.2f nodes/sec) and %d relationships (%.2f rels/sec) in %.2f seconds. " +
                            "Final counts: %d nodes, %d relationships",
                            nodeCount, (nodeCount * 1000.0 / totalDuration),
                            relationshipCount, (relationshipCount * 1000.0 / totalDuration),
                            totalDuration / 1000.0,
                            finalNodeCount, finalRelCount
                    ))
                    .build();
            
            log.info("Bulk insert completed: {} ms", totalDuration);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error during bulk insert", e);
            return ResponseEntity.internalServerError().body(
                    PerformanceResult.builder()
                            .database(graphService.getDatabaseType())
                            .operation("BULK_INSERT_LARGE_DATASET")
                            .executionTimeMs(-1)
                            .recordCount(0)
                            .additionalInfo("Error: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/insert-nodes-only")
    public ResponseEntity<PerformanceResult> insertNodesOnly(
            @RequestParam(defaultValue = "50000") int nodeCount) {
        
        long startTime = System.currentTimeMillis();
        
        log.info("Starting bulk node insert: {} nodes", nodeCount);
        
        try {
            List<String> nodeIds = createNodesBatched(nodeCount);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            long finalNodeCount = graphService.countNodes();
            
            PerformanceResult result = PerformanceResult.builder()
                    .database(graphService.getDatabaseType())
                    .operation("BULK_INSERT_NODES")
                    .executionTimeMs(duration)
                    .recordCount(nodeCount)
                    .additionalInfo(String.format(
                            "Created %d nodes in %.2f seconds (%.2f nodes/sec). Final count: %d",
                            nodeCount, duration / 1000.0, (nodeCount * 1000.0 / duration), finalNodeCount
                    ))
                    .build();
            
            log.info("Bulk node insert completed: {} ms", duration);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error during bulk node insert", e);
            return ResponseEntity.internalServerError().body(
                    PerformanceResult.builder()
                            .database(graphService.getDatabaseType())
                            .operation("BULK_INSERT_NODES")
                            .executionTimeMs(-1)
                            .recordCount(0)
                            .additionalInfo("Error: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/insert-relationships-only")
    public ResponseEntity<PerformanceResult> insertRelationshipsOnly(
            @RequestParam(defaultValue = "200000") int relationshipCount) {
        
        log.info("Checking existing nodes for relationships...");
        List<String> existingNodeIds = graphService.getAllNodes().stream()
                .map(node -> node.getId())
                .toList();
        
        if (existingNodeIds.size() < 2) {
            return ResponseEntity.badRequest().body(
                    PerformanceResult.builder()
                            .database(graphService.getDatabaseType())
                            .operation("BULK_INSERT_RELATIONSHIPS")
                            .executionTimeMs(-1)
                            .recordCount(0)
                            .additionalInfo("Error: Need at least 2 nodes in database. Found: " + existingNodeIds.size())
                            .build()
            );
        }
        
        long startTime = System.currentTimeMillis();
        
        log.info("Starting bulk relationship insert: {} relationships", relationshipCount);
        
        try {
            createRelationshipsBatched(existingNodeIds, relationshipCount);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            long finalRelCount = graphService.countRelationships();
            
            PerformanceResult result = PerformanceResult.builder()
                    .database(graphService.getDatabaseType())
                    .operation("BULK_INSERT_RELATIONSHIPS")
                    .executionTimeMs(duration)
                    .recordCount(relationshipCount)
                    .additionalInfo(String.format(
                            "Created %d relationships in %.2f seconds (%.2f rels/sec). Final count: %d",
                            relationshipCount, duration / 1000.0, (relationshipCount * 1000.0 / duration), finalRelCount
                    ))
                    .build();
            
            log.info("Bulk relationship insert completed: {} ms", duration);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error during bulk relationship insert", e);
            return ResponseEntity.internalServerError().body(
                    PerformanceResult.builder()
                            .database(graphService.getDatabaseType())
                            .operation("BULK_INSERT_RELATIONSHIPS")
                            .executionTimeMs(-1)
                            .recordCount(0)
                            .additionalInfo("Error: " + e.getMessage())
                            .build()
            );
        }
    }

    private List<String> createNodesBatched(int totalNodes) {
        List<String> allNodeIds = new ArrayList<>();
        int batches = (int) Math.ceil((double) totalNodes / BATCH_SIZE);
        
        for (int i = 0; i < batches; i++) {
            int batchStart = i * BATCH_SIZE;
            int batchEnd = Math.min(batchStart + BATCH_SIZE, totalNodes);
            int batchSize = batchEnd - batchStart;
            
            List<String> batchNodeIds = new ArrayList<>(batchSize);
            for (int j = 0; j < batchSize; j++) {
                batchNodeIds.add("node-" + UUID.randomUUID());
            }
            
            log.info("Creating node batch {}/{} ({} nodes)...", i + 1, batches, batchSize);
            graphService.createNodesBatch(batchNodeIds);
            allNodeIds.addAll(batchNodeIds);
            
            // Clear batch list to free memory
            batchNodeIds.clear();
            
            // Periodic garbage collection hint every 10 batches
            if ((i + 1) % 10 == 0) {
                System.gc();
            }
        }
        
        return allNodeIds;
    }

    private void createRelationshipsBatched(List<String> nodeIds, int totalRelationships) {
        Random random = new Random();
        int nodeCount = nodeIds.size();
        int batches = (int) Math.ceil((double) totalRelationships / BATCH_SIZE);
        
        for (int i = 0; i < batches; i++) {
            int batchStart = i * BATCH_SIZE;
            int batchEnd = Math.min(batchStart + BATCH_SIZE, totalRelationships);
            int batchSize = batchEnd - batchStart;
            
            List<RelationshipBatch> batchRelationships = new ArrayList<>(batchSize);
            for (int j = 0; j < batchSize; j++) {
                String sourceId = nodeIds.get(random.nextInt(nodeCount));
                String targetId = nodeIds.get(random.nextInt(nodeCount));
                Long relationTypeId = (long) (random.nextInt(10) + 1);
                
                batchRelationships.add(new RelationshipBatch(sourceId, targetId, relationTypeId));
            }
            
            log.info("Creating relationship batch {}/{} ({} relationships)...", i + 1, batches, batchSize);
            graphService.createRelationshipsBatch(batchRelationships);
            
            // Clear batch list to free memory
            batchRelationships.clear();
            
            // Periodic garbage collection hint every 10 batches
            if ((i + 1) % 10 == 0) {
                System.gc();
            }
        }
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<String> clearAll() {
        log.info("Clearing all data...");
        long startTime = System.currentTimeMillis();
        
        try {
            graphService.deleteAllNodes();
            long duration = System.currentTimeMillis() - startTime;
            
            String message = String.format("All data cleared in %d ms using %s", 
                    duration, graphService.getDatabaseType());
            log.info(message);
            
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("Error clearing data", e);
            return ResponseEntity.internalServerError()
                    .body("Error clearing data: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<String> getStats() {
        long nodeCount = graphService.countNodes();
        long relationshipCount = graphService.countRelationships();
        String database = graphService.getDatabaseType();

        String stats = String.format(
                "Database: %s\nNodes: %,d\nRelationships: %,d\nTotal Records: %,d",
                database, nodeCount, relationshipCount, nodeCount + relationshipCount
        );

        return ResponseEntity.ok(stats);
    }
}

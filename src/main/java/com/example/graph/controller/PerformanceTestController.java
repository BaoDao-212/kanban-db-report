package com.example.graph.controller;

import com.example.graph.dto.PerformanceResult;
import com.example.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Slf4j
public class PerformanceTestController {

    private final GraphService graphService;

    @PostMapping("/test/create-nodes")
    public ResponseEntity<PerformanceResult> testCreateNodes(@RequestParam(defaultValue = "1000") int count) {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < count; i++) {
            String nodeId = "node-" + UUID.randomUUID();
            graphService.createNode(nodeId);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        PerformanceResult result = PerformanceResult.builder()
                .database(graphService.getDatabaseType())
                .operation("CREATE_NODES")
                .executionTimeMs(duration)
                .recordCount(count)
                .additionalInfo(String.format("Average: %.2f ms/node", (double) duration / count))
                .build();

        log.info("Performance test - Create {} nodes: {} ms", count, duration);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/test/create-relationships")
    public ResponseEntity<PerformanceResult> testCreateRelationships(@RequestParam(defaultValue = "1000") int count) {
        List<String> nodeIds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String nodeId = "rel-test-node-" + i;
            graphService.createNode(nodeId);
            nodeIds.add(nodeId);
        }

        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < count - 1; i++) {
            graphService.createRelationship(nodeIds.get(i), nodeIds.get(i + 1), (long) (i % 10));
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        PerformanceResult result = PerformanceResult.builder()
                .database(graphService.getDatabaseType())
                .operation("CREATE_RELATIONSHIPS")
                .executionTimeMs(duration)
                .recordCount(count - 1)
                .additionalInfo(String.format("Average: %.2f ms/relationship", (double) duration / (count - 1)))
                .build();

        log.info("Performance test - Create {} relationships: {} ms", count - 1, duration);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/test/read-all-nodes")
    public ResponseEntity<PerformanceResult> testReadAllNodes() {
        long startTime = System.currentTimeMillis();
        
        List<?> nodes = graphService.getAllNodes();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        PerformanceResult result = PerformanceResult.builder()
                .database(graphService.getDatabaseType())
                .operation("READ_ALL_NODES")
                .executionTimeMs(duration)
                .recordCount(nodes.size())
                .additionalInfo(String.format("Retrieved %d nodes", nodes.size()))
                .build();

        log.info("Performance test - Read all nodes: {} ms ({} nodes)", duration, nodes.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/test/read-by-relation-type")
    public ResponseEntity<PerformanceResult> testReadByRelationType(@RequestParam(defaultValue = "1") Long relationTypeId) {
        long startTime = System.currentTimeMillis();
        
        List<?> nodes = graphService.getNodesByRelationTypeId(relationTypeId);
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        PerformanceResult result = PerformanceResult.builder()
                .database(graphService.getDatabaseType())
                .operation("READ_BY_RELATION_TYPE")
                .executionTimeMs(duration)
                .recordCount(nodes.size())
                .additionalInfo(String.format("RelationType: %d, Retrieved %d nodes", relationTypeId, nodes.size()))
                .build();

        log.info("Performance test - Read by relation type {}: {} ms ({} nodes)", relationTypeId, duration, nodes.size());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/test/full-suite")
    public ResponseEntity<List<PerformanceResult>> runFullTestSuite(
            @RequestParam(defaultValue = "1000") int nodeCount,
            @RequestParam(defaultValue = "500") int relationshipCount) {
        
        List<PerformanceResult> results = new ArrayList<>();

        graphService.deleteAllNodes();

        PerformanceResult createNodesResult = testCreateNodes(nodeCount).getBody();
        results.add(createNodesResult);

        PerformanceResult createRelationshipsResult = testCreateRelationships(relationshipCount).getBody();
        results.add(createRelationshipsResult);

        PerformanceResult readAllResult = testReadAllNodes().getBody();
        results.add(readAllResult);

        PerformanceResult readByTypeResult = testReadByRelationType(1L).getBody();
        results.add(readByTypeResult);

        log.info("Full performance test suite completed");
        return ResponseEntity.ok(results);
    }

    @GetMapping("/stats")
    public ResponseEntity<String> getStats() {
        long nodeCount = graphService.countNodes();
        long relationshipCount = graphService.countRelationships();
        String database = graphService.getDatabaseType();

        String stats = String.format(
                "Database: %s\nNodes: %d\nRelationships: %d",
                database, nodeCount, relationshipCount
        );

        return ResponseEntity.ok(stats);
    }
}

package com.example.graph.controller;

import com.example.graph.domain.CiNode;
import com.example.graph.dto.CreateNodeRequest;
import com.example.graph.dto.CreateRelationshipRequest;
import com.example.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/graph")
@RequiredArgsConstructor
@Slf4j
public class GraphController {

    private final GraphService graphService;

    @PostMapping("/nodes")
    public ResponseEntity<CiNode> createNode(@RequestBody CreateNodeRequest request) {
        CiNode node = graphService.createNode(request.getId());
        return ResponseEntity.ok(node);
    }

    @PostMapping("/relationships")
    public ResponseEntity<Void> createRelationship(@RequestBody CreateRelationshipRequest request) {
        graphService.createRelationship(
                request.getSourceId(),
                request.getTargetId(),
                request.getRelationTypeId()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/nodes/{id}")
    public ResponseEntity<CiNode> getNode(@PathVariable String id) {
        return graphService.getNode(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nodes")
    public ResponseEntity<List<CiNode>> getAllNodes() {
        return ResponseEntity.ok(graphService.getAllNodes());
    }

    @DeleteMapping("/nodes/{id}")
    public ResponseEntity<Void> deleteNode(@PathVariable String id) {
        graphService.deleteNode(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/nodes")
    public ResponseEntity<Void> deleteAllNodes() {
        graphService.deleteAllNodes();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats/nodes/count")
    public ResponseEntity<Long> countNodes() {
        return ResponseEntity.ok(graphService.countNodes());
    }

    @GetMapping("/stats/relationships/count")
    public ResponseEntity<Long> countRelationships() {
        return ResponseEntity.ok(graphService.countRelationships());
    }

    @GetMapping("/nodes/by-relation-type/{relationTypeId}")
    public ResponseEntity<List<CiNode>> getNodesByRelationType(@PathVariable Long relationTypeId) {
        return ResponseEntity.ok(graphService.getNodesByRelationTypeId(relationTypeId));
    }

    @GetMapping("/database-type")
    public ResponseEntity<String> getDatabaseType() {
        return ResponseEntity.ok(graphService.getDatabaseType());
    }
}

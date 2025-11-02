package com.example.graph.service.impl;

import com.example.graph.client.TigerGraphClient;
import com.example.graph.domain.CiNode;
import com.example.graph.domain.CiRelationship;
import com.example.graph.service.GraphService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@ConditionalOnProperty(name = "graph.database.type", havingValue = "tigergraph")
@RequiredArgsConstructor
@Slf4j
public class TigerGraphService implements GraphService {

    private final TigerGraphClient tigerGraphClient;
    private static final String VERTEX_TYPE = "CiNode";
    private static final String EDGE_TYPE = "RELATES_TO";

    @Override
    public CiNode createNode(String id) {
        try {
            tigerGraphClient.upsertVertex(VERTEX_TYPE, id);
            return CiNode.builder().id(id).build();
        } catch (Exception e) {
            log.error("Error creating node in TigerGraph", e);
            throw new RuntimeException("Failed to create node", e);
        }
    }

    @Override
    public void createRelationship(String sourceId, String targetId, Long relationTypeId) {
        try {
            tigerGraphClient.upsertEdge(VERTEX_TYPE, sourceId, EDGE_TYPE, VERTEX_TYPE, targetId, relationTypeId);
        } catch (Exception e) {
            log.error("Error creating relationship in TigerGraph", e);
            throw new RuntimeException("Failed to create relationship", e);
        }
    }

    @Override
    public Optional<CiNode> getNode(String id) {
        try {
            JsonNode vertex = tigerGraphClient.getVertex(VERTEX_TYPE, id);
            if (vertex == null) {
                return Optional.empty();
            }
            return Optional.of(jsonNodeToCiNode(vertex));
        } catch (Exception e) {
            log.error("Error getting node from TigerGraph", e);
            return Optional.empty();
        }
    }

    @Override
    public List<CiNode> getAllNodes() {
        try {
            List<JsonNode> vertices = tigerGraphClient.getAllVertices(VERTEX_TYPE);
            List<CiNode> nodes = new ArrayList<>();
            for (JsonNode vertex : vertices) {
                nodes.add(jsonNodeToCiNode(vertex));
            }
            return nodes;
        } catch (Exception e) {
            log.error("Error getting all nodes from TigerGraph", e);
            return new ArrayList<>();
        }
    }

    @Override
    public void deleteNode(String id) {
        try {
            tigerGraphClient.deleteVertex(VERTEX_TYPE, id);
        } catch (Exception e) {
            log.error("Error deleting node from TigerGraph", e);
            throw new RuntimeException("Failed to delete node", e);
        }
    }

    @Override
    public void deleteAllNodes() {
        try {
            tigerGraphClient.deleteAllVertices(VERTEX_TYPE);
        } catch (Exception e) {
            log.error("Error deleting all nodes from TigerGraph", e);
            throw new RuntimeException("Failed to delete all nodes", e);
        }
    }

    @Override
    public long countNodes() {
        try {
            return tigerGraphClient.countVertices(VERTEX_TYPE);
        } catch (Exception e) {
            log.error("Error counting nodes in TigerGraph", e);
            return 0;
        }
    }

    @Override
    public long countRelationships() {
        try {
            return tigerGraphClient.countEdges(EDGE_TYPE);
        } catch (Exception e) {
            log.error("Error counting relationships in TigerGraph", e);
            return 0;
        }
    }

    @Override
    public List<CiNode> getNodesByRelationTypeId(Long relationTypeId) {
        log.warn("getNodesByRelationTypeId not fully implemented for TigerGraph - requires custom GSQL query");
        return new ArrayList<>();
    }

    @Override
    public String getDatabaseType() {
        return "TigerGraph";
    }

    @Override
    public List<CiNode> createNodesBatch(List<String> nodeIds) {
        try {
            tigerGraphClient.upsertVerticesBatch(VERTEX_TYPE, nodeIds);
            List<CiNode> nodes = new ArrayList<>();
            for (String id : nodeIds) {
                nodes.add(CiNode.builder().id(id).build());
            }
            return nodes;
        } catch (Exception e) {
            log.error("Error creating nodes batch in TigerGraph", e);
            throw new RuntimeException("Failed to create nodes batch", e);
        }
    }

    @Override
    public void createRelationshipsBatch(List<RelationshipBatch> relationships) {
        try {
            List<Map<String, Object>> edgeData = new ArrayList<>();
            for (RelationshipBatch rel : relationships) {
                Map<String, Object> edge = new HashMap<>();
                edge.put("sourceId", rel.sourceId);
                edge.put("targetId", rel.targetId);
                edge.put("relationTypeId", rel.relationTypeId);
                edgeData.add(edge);
            }
            tigerGraphClient.upsertEdgesBatch(VERTEX_TYPE, EDGE_TYPE, VERTEX_TYPE, edgeData);
        } catch (Exception e) {
            log.error("Error creating relationships batch in TigerGraph", e);
            throw new RuntimeException("Failed to create relationships batch", e);
        }
    }

    private CiNode jsonNodeToCiNode(JsonNode vertex) {
        String id = vertex.has("v_id") ? vertex.get("v_id").asText() : 
                   (vertex.has("id") ? vertex.get("id").asText() : null);
        
        CiNode node = CiNode.builder()
                .id(id)
                .build();

        if (vertex.has("edges")) {
            Set<CiRelationship> relationships = new HashSet<>();
            JsonNode edges = vertex.get("edges");
            if (edges.isObject()) {
                JsonNode relatesTo = edges.get(EDGE_TYPE);
                if (relatesTo != null && relatesTo.isArray()) {
                    for (JsonNode edge : relatesTo) {
                        String targetId = edge.has("to_id") ? edge.get("to_id").asText() : null;
                        Long relationTypeId = null;
                        
                        if (edge.has("attributes")) {
                            JsonNode attributes = edge.get("attributes");
                            if (attributes.has("relationTypeId")) {
                                relationTypeId = attributes.get("relationTypeId").asLong();
                            }
                        }

                        CiNode target = CiNode.builder().id(targetId).build();
                        CiRelationship relationship = CiRelationship.builder()
                                .relationTypeId(relationTypeId)
                                .target(target)
                                .build();
                        relationships.add(relationship);
                    }
                }
            }
            node.setOutgoingRelations(relationships);
        }

        return node;
    }
}

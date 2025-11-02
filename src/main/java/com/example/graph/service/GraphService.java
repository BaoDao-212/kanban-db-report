package com.example.graph.service;

import com.example.graph.domain.CiNode;

import java.util.List;
import java.util.Optional;

public interface GraphService {
    
    CiNode createNode(String id);
    
    void createRelationship(String sourceId, String targetId, Long relationTypeId);
    
    Optional<CiNode> getNode(String id);
    
    List<CiNode> getAllNodes();
    
    void deleteNode(String id);
    
    void deleteAllNodes();
    
    long countNodes();
    
    long countRelationships();
    
    List<CiNode> getNodesByRelationTypeId(Long relationTypeId);
    
    String getDatabaseType();
    
    // Bulk insert methods for optimization
    List<CiNode> createNodesBatch(List<String> nodeIds);
    
    void createRelationshipsBatch(List<RelationshipBatch> relationships);
    
    class RelationshipBatch {
        public String sourceId;
        public String targetId;
        public Long relationTypeId;
        
        public RelationshipBatch(String sourceId, String targetId, Long relationTypeId) {
            this.sourceId = sourceId;
            this.targetId = targetId;
            this.relationTypeId = relationTypeId;
        }
    }
}

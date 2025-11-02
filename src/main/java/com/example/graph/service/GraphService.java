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
}

package com.example.graph.service.impl;

import com.example.graph.domain.CiNode;
import com.example.graph.domain.CiRelationship;
import com.example.graph.repository.CiNodeRepository;
import com.example.graph.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@ConditionalOnProperty(name = "graph.database.type", havingValue = "neo4j", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class Neo4jGraphService implements GraphService {

    private final CiNodeRepository ciNodeRepository;

    @Override
    @Transactional
    public CiNode createNode(String id) {
        CiNode node = CiNode.builder()
                .id(id)
                .build();
        return ciNodeRepository.save(node);
    }

    @Override
    @Transactional
    public void createRelationship(String sourceId, String targetId, Long relationTypeId) {
        Optional<CiNode> sourceOpt = ciNodeRepository.findById(sourceId);
        Optional<CiNode> targetOpt = ciNodeRepository.findById(targetId);

        if (sourceOpt.isEmpty() || targetOpt.isEmpty()) {
            throw new RuntimeException("Source or target node not found");
        }

        CiNode source = sourceOpt.get();
        CiNode target = targetOpt.get();

        CiRelationship relationship = CiRelationship.builder()
                .relationTypeId(relationTypeId)
                .target(target)
                .build();

        source.getOutgoingRelations().add(relationship);
        ciNodeRepository.save(source);
    }

    @Override
    public Optional<CiNode> getNode(String id) {
        return ciNodeRepository.findById(id);
    }

    @Override
    public List<CiNode> getAllNodes() {
        return ciNodeRepository.findAll();
    }

    @Override
    @Transactional
    public void deleteNode(String id) {
        ciNodeRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteAllNodes() {
        ciNodeRepository.deleteAll();
    }

    @Override
    public long countNodes() {
        return ciNodeRepository.count();
    }

    @Override
    public long countRelationships() {
        return ciNodeRepository.countRelationships();
    }

    @Override
    public List<CiNode> getNodesByRelationTypeId(Long relationTypeId) {
        return ciNodeRepository.findByRelationTypeId(relationTypeId);
    }

    @Override
    public String getDatabaseType() {
        return "Neo4j";
    }

    @Override
    @Transactional
    public List<CiNode> createNodesBatch(List<String> nodeIds) {
        List<CiNode> nodes = new ArrayList<>();
        for (String id : nodeIds) {
            CiNode node = CiNode.builder()
                    .id(id)
                    .build();
            nodes.add(node);
        }
        return ciNodeRepository.saveAll(nodes);
    }

    @Override
    @Transactional
    public void createRelationshipsBatch(List<RelationshipBatch> relationships) {
        Map<String, CiNode> nodeCache = new HashMap<>();
        
        for (RelationshipBatch rel : relationships) {
            if (!nodeCache.containsKey(rel.sourceId)) {
                ciNodeRepository.findById(rel.sourceId).ifPresent(node -> nodeCache.put(rel.sourceId, node));
            }
            if (!nodeCache.containsKey(rel.targetId)) {
                ciNodeRepository.findById(rel.targetId).ifPresent(node -> nodeCache.put(rel.targetId, node));
            }
        }

        for (RelationshipBatch rel : relationships) {
            CiNode source = nodeCache.get(rel.sourceId);
            CiNode target = nodeCache.get(rel.targetId);
            
            if (source != null && target != null) {
                CiRelationship relationship = CiRelationship.builder()
                        .relationTypeId(rel.relationTypeId)
                        .target(target)
                        .build();
                source.getOutgoingRelations().add(relationship);
            }
        }
        
        ciNodeRepository.saveAll(nodeCache.values());
    }
}

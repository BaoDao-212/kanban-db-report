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

import java.util.List;
import java.util.Optional;

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
}

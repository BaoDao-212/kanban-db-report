package com.example.graph.repository;

import com.example.graph.domain.CiNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CiNodeRepository extends Neo4jRepository<CiNode, String> {
    
    @Query("MATCH (n:CiNode)-[r:RELATES_TO]->(m:CiNode) WHERE r.relationTypeId = $relationTypeId RETURN n, collect(r), collect(m)")
    List<CiNode> findByRelationTypeId(@Param("relationTypeId") Long relationTypeId);
    
    @Query("MATCH ()-[r:RELATES_TO]->() RETURN count(r)")
    long countRelationships();
}

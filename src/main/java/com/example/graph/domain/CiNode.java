package com.example.graph.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

@Node
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CiNode {

    @Id
    private String id;

    @Relationship(type = "RELATES_TO", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<CiRelationship> outgoingRelations = new HashSet<>();
}

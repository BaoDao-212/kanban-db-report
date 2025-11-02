package com.example.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRelationshipRequest {
    private String sourceId;
    private String targetId;
    private Long relationTypeId;
}

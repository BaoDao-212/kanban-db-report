package com.example.graph.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceResult {
    private String database;
    private String operation;
    private long executionTimeMs;
    private long recordCount;
    private String additionalInfo;
}

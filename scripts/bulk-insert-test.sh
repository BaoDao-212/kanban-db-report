#!/bin/bash

# Bulk Insert Performance Test Script
# Tests both Neo4j and TigerGraph with 50K nodes + 200K relationships

set -e

NODE_COUNT=50000
REL_COUNT=200000
APP_JAR="target/graph-performance-comparison-1.0.0.jar"

echo "=================================================="
echo "  Bulk Insert Performance Comparison"
echo "=================================================="
echo "Configuration:"
echo "  - Nodes: ${NODE_COUNT}"
echo "  - Relationships: ${REL_COUNT}"
echo "  - Total Records: $((NODE_COUNT + REL_COUNT))"
echo "=================================================="
echo ""

# Check if JAR exists
if [ ! -f "$APP_JAR" ]; then
    echo "Error: Application JAR not found at $APP_JAR"
    echo "Please run: mvn clean package"
    exit 1
fi

# Check if Docker containers are running
echo "Checking Docker containers..."
if ! docker ps | grep -q neo4j-graph; then
    echo "Error: Neo4j container is not running"
    echo "Please run: docker-compose up -d neo4j"
    exit 1
fi

if ! docker ps | grep -q tigergraph-graph; then
    echo "Error: TigerGraph container is not running"
    echo "Please run: docker-compose up -d tigergraph"
    exit 1
fi

echo "✓ Docker containers are running"
echo ""

# Function to wait for application to start
wait_for_app() {
    local max_wait=60
    local counter=0
    
    echo "Waiting for application to start..."
    while [ $counter -lt $max_wait ]; do
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo "✓ Application is ready"
            return 0
        fi
        counter=$((counter + 1))
        sleep 1
    done
    
    echo "Error: Application failed to start within ${max_wait} seconds"
    return 1
}

# Test Neo4j
echo "=================================================="
echo "  Testing Neo4j"
echo "=================================================="
echo ""

echo "Starting application with Neo4j profile..."
java -jar "$APP_JAR" --spring.profiles.active=neo4j > neo4j-app.log 2>&1 &
NEO4J_PID=$!
echo "Application PID: $NEO4J_PID"

if ! wait_for_app; then
    kill $NEO4J_PID 2>/dev/null || true
    echo "Check neo4j-app.log for errors"
    exit 1
fi

echo ""
echo "Running bulk insert test..."
START_TIME=$(date +%s)

curl -s -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=${NODE_COUNT}&relationshipCount=${REL_COUNT}" \
    -H "Content-Type: application/json" \
    | tee neo4j-result.json | jq '.'

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "Getting statistics..."
curl -s -X GET "http://localhost:8080/api/bulk/stats"

echo ""
echo ""
echo "Neo4j test completed in ${DURATION} seconds"
echo "Results saved to: neo4j-result.json"
echo "Logs saved to: neo4j-app.log"

echo ""
echo "Stopping application..."
kill $NEO4J_PID 2>/dev/null || true
sleep 5

# Test TigerGraph
echo ""
echo "=================================================="
echo "  Testing TigerGraph"
echo "=================================================="
echo ""

echo "Starting application with TigerGraph profile..."
java -jar "$APP_JAR" --spring.profiles.active=tigergraph > tigergraph-app.log 2>&1 &
TIGERGRAPH_PID=$!
echo "Application PID: $TIGERGRAPH_PID"

if ! wait_for_app; then
    kill $TIGERGRAPH_PID 2>/dev/null || true
    echo "Check tigergraph-app.log for errors"
    exit 1
fi

echo ""
echo "Running bulk insert test..."
START_TIME=$(date +%s)

curl -s -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=${NODE_COUNT}&relationshipCount=${REL_COUNT}" \
    -H "Content-Type: application/json" \
    | tee tigergraph-result.json | jq '.'

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo "Getting statistics..."
curl -s -X GET "http://localhost:8080/api/bulk/stats"

echo ""
echo ""
echo "TigerGraph test completed in ${DURATION} seconds"
echo "Results saved to: tigergraph-result.json"
echo "Logs saved to: tigergraph-app.log"

echo ""
echo "Stopping application..."
kill $TIGERGRAPH_PID 2>/dev/null || true

# Summary
echo ""
echo "=================================================="
echo "  Test Summary"
echo "=================================================="
echo ""
echo "Results:"
echo "  - Neo4j: neo4j-result.json"
echo "  - TigerGraph: tigergraph-result.json"
echo ""
echo "Logs:"
echo "  - Neo4j: neo4j-app.log"
echo "  - TigerGraph: tigergraph-app.log"
echo ""
echo "Comparison:"
if command -v jq > /dev/null 2>&1; then
    NEO4J_TIME=$(jq -r '.executionTimeMs' neo4j-result.json 2>/dev/null || echo "N/A")
    TIGER_TIME=$(jq -r '.executionTimeMs' tigergraph-result.json 2>/dev/null || echo "N/A")
    
    if [ "$NEO4J_TIME" != "N/A" ] && [ "$TIGER_TIME" != "N/A" ]; then
        echo "  - Neo4j execution time: ${NEO4J_TIME} ms"
        echo "  - TigerGraph execution time: ${TIGER_TIME} ms"
        
        if [ "$NEO4J_TIME" -lt "$TIGER_TIME" ]; then
            DIFF=$((TIGER_TIME - NEO4J_TIME))
            PCT=$((DIFF * 100 / NEO4J_TIME))
            echo "  - Neo4j was faster by ${DIFF} ms (${PCT}%)"
        else
            DIFF=$((NEO4J_TIME - TIGER_TIME))
            PCT=$((DIFF * 100 / TIGER_TIME))
            echo "  - TigerGraph was faster by ${DIFF} ms (${PCT}%)"
        fi
    fi
fi
echo ""
echo "=================================================="
echo "  Test completed successfully!"
echo "=================================================="

#!/bin/bash

# Performance Comparison Script for Neo4j vs TigerGraph

set -e

echo "=========================================="
echo "Graph Database Performance Comparison"
echo "Neo4j vs TigerGraph"
echo "=========================================="
echo ""

# Configuration
NODE_COUNT=${1:-5000}
REL_COUNT=${2:-2500}
APP_JAR="target/graph-performance-comparison-1.0.0.jar"
WAIT_TIME=15

echo "Test Configuration:"
echo "  - Nodes: $NODE_COUNT"
echo "  - Relationships: $REL_COUNT"
echo "  - Wait time: ${WAIT_TIME}s"
echo ""

# Check if JAR exists
if [ ! -f "$APP_JAR" ]; then
    echo "ERROR: JAR file not found at $APP_JAR"
    echo "Please run: mvn clean package"
    exit 1
fi

# Function to wait for app to start
wait_for_app() {
    local max_attempts=30
    local attempt=0
    
    echo "Waiting for application to start..."
    while [ $attempt -lt $max_attempts ]; do
        if curl -s http://localhost:8080/api/graph/database-type > /dev/null 2>&1; then
            echo "Application is ready!"
            return 0
        fi
        attempt=$((attempt + 1))
        sleep 1
    done
    
    echo "ERROR: Application failed to start"
    return 1
}

# Test Neo4j
echo "=========================================="
echo "Testing Neo4j"
echo "=========================================="

echo "Starting application with Neo4j profile..."
java -jar "$APP_JAR" --spring.profiles.active=neo4j > neo4j.log 2>&1 &
NEO4J_PID=$!

if ! wait_for_app; then
    kill $NEO4J_PID 2>/dev/null || true
    exit 1
fi

echo "Clearing existing data..."
curl -s -X DELETE http://localhost:8080/api/graph/nodes > /dev/null

echo "Running performance tests..."
curl -s -X POST "http://localhost:8080/api/performance/test/full-suite?nodeCount=$NODE_COUNT&relationshipCount=$REL_COUNT" \
    -H "Content-Type: application/json" \
    | jq '.' > neo4j-results.json

echo "✓ Neo4j results saved to: neo4j-results.json"

echo "Stopping Neo4j instance..."
kill $NEO4J_PID
wait $NEO4J_PID 2>/dev/null || true
sleep 3

echo ""
echo "=========================================="
echo "Testing TigerGraph"
echo "=========================================="

echo "Starting application with TigerGraph profile..."
java -jar "$APP_JAR" --spring.profiles.active=tigergraph > tigergraph.log 2>&1 &
TIGER_PID=$!

if ! wait_for_app; then
    kill $TIGER_PID 2>/dev/null || true
    exit 1
fi

echo "Clearing existing data..."
curl -s -X DELETE http://localhost:8080/api/graph/nodes > /dev/null

echo "Running performance tests..."
curl -s -X POST "http://localhost:8080/api/performance/test/full-suite?nodeCount=$NODE_COUNT&relationshipCount=$REL_COUNT" \
    -H "Content-Type: application/json" \
    | jq '.' > tigergraph-results.json

echo "✓ TigerGraph results saved to: tigergraph-results.json"

echo "Stopping TigerGraph instance..."
kill $TIGER_PID
wait $TIGER_PID 2>/dev/null || true

echo ""
echo "=========================================="
echo "Results Summary"
echo "=========================================="

echo ""
echo "Neo4j Results:"
echo "--------------"
jq -r '.[] | "\(.operation): \(.executionTimeMs)ms (\(.additionalInfo))"' neo4j-results.json

echo ""
echo "TigerGraph Results:"
echo "-------------------"
jq -r '.[] | "\(.operation): \(.executionTimeMs)ms (\(.additionalInfo))"' tigergraph-results.json

echo ""
echo "=========================================="
echo "Comparison Complete!"
echo "=========================================="
echo ""
echo "Detailed results available in:"
echo "  - neo4j-results.json"
echo "  - tigergraph-results.json"
echo ""
echo "Logs available in:"
echo "  - neo4j.log"
echo "  - tigergraph.log"

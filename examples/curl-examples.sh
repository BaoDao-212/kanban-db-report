#!/bin/bash

# Curl Examples for Graph Performance Comparison API

BASE_URL="http://localhost:8080"

echo "=========================================="
echo "Graph Performance API - Curl Examples"
echo "=========================================="
echo ""

# Check database type
echo "1. Check current database:"
curl -s $BASE_URL/api/graph/database-type
echo -e "\n"

# Create nodes
echo "2. Create nodes:"
curl -s -X POST $BASE_URL/api/graph/nodes \
  -H "Content-Type: application/json" \
  -d '{"id": "user-1"}' | jq '.'

curl -s -X POST $BASE_URL/api/graph/nodes \
  -H "Content-Type: application/json" \
  -d '{"id": "user-2"}' | jq '.'

curl -s -X POST $BASE_URL/api/graph/nodes \
  -H "Content-Type: application/json" \
  -d '{"id": "user-3"}' | jq '.'

echo ""

# Create relationships
echo "3. Create relationships:"
curl -s -X POST $BASE_URL/api/graph/relationships \
  -H "Content-Type: application/json" \
  -d '{"sourceId": "user-1", "targetId": "user-2", "relationTypeId": 1}' | jq '.'

curl -s -X POST $BASE_URL/api/graph/relationships \
  -H "Content-Type: application/json" \
  -d '{"sourceId": "user-2", "targetId": "user-3", "relationTypeId": 2}' | jq '.'

curl -s -X POST $BASE_URL/api/graph/relationships \
  -H "Content-Type: application/json" \
  -d '{"sourceId": "user-1", "targetId": "user-3", "relationTypeId": 1}' | jq '.'

echo ""

# Get node
echo "4. Get node by ID:"
curl -s $BASE_URL/api/graph/nodes/user-1 | jq '.'
echo ""

# Get all nodes
echo "5. Get all nodes:"
curl -s $BASE_URL/api/graph/nodes | jq '.'
echo ""

# Count nodes and relationships
echo "6. Statistics:"
echo -n "Nodes: "
curl -s $BASE_URL/api/graph/stats/nodes/count
echo ""
echo -n "Relationships: "
curl -s $BASE_URL/api/graph/stats/relationships/count
echo -e "\n"

# Get nodes by relation type
echo "7. Get nodes by relation type 1:"
curl -s $BASE_URL/api/graph/nodes/by-relation-type/1 | jq '.'
echo ""

# Performance tests
echo "8. Performance test - Create 100 nodes:"
curl -s -X POST "$BASE_URL/api/performance/test/create-nodes?count=100" | jq '.'
echo ""

echo "9. Performance test - Create 50 relationships:"
curl -s -X POST "$BASE_URL/api/performance/test/create-relationships?count=50" | jq '.'
echo ""

echo "10. Performance test - Read all nodes:"
curl -s $BASE_URL/api/performance/test/read-all-nodes | jq '.'
echo ""

# Full suite (commented out as it's intensive)
# echo "11. Full test suite (5000 nodes, 2500 relationships):"
# curl -s -X POST "$BASE_URL/api/performance/test/full-suite?nodeCount=5000&relationshipCount=2500" | jq '.'

echo "=========================================="
echo "Examples complete!"
echo "=========================================="

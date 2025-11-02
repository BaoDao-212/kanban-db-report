#!/bin/bash

# Database Setup Script

set -e

echo "=========================================="
echo "Database Setup Script"
echo "Neo4j & TigerGraph"
echo "=========================================="
echo ""

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null && ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed"
    exit 1
fi

COMPOSE_CMD="docker-compose"
if ! command -v docker-compose &> /dev/null; then
    COMPOSE_CMD="docker compose"
fi

echo "Starting databases with Docker Compose..."
cd "$(dirname "$0")/.."
$COMPOSE_CMD up -d

echo ""
echo "Waiting for databases to start..."
sleep 10

# Check Neo4j
echo ""
echo "Checking Neo4j..."
if curl -s http://localhost:7474 > /dev/null; then
    echo "✓ Neo4j is running"
    echo "  - Browser: http://localhost:7474"
    echo "  - Bolt: bolt://localhost:7687"
    echo "  - Username: neo4j"
    echo "  - Password: password"
else
    echo "✗ Neo4j is not responding"
fi

# Check TigerGraph
echo ""
echo "Checking TigerGraph..."
if curl -s http://localhost:9000/echo > /dev/null; then
    echo "✓ TigerGraph is running"
    echo "  - REST API: http://localhost:9000"
    echo "  - GraphStudio: http://localhost:14240"
else
    echo "⚠ TigerGraph may still be starting (this can take 2-3 minutes)"
    echo "  Run this script again to verify"
fi

echo ""
echo "=========================================="
echo "Setting up TigerGraph schema..."
echo "=========================================="
echo ""
echo "To setup TigerGraph schema, run:"
echo "  docker exec -it tigergraph-graph bash"
echo "  gsql /tmp/setup.gsql"
echo ""
echo "Or wait a moment and run:"
echo "  ./scripts/setup-tigergraph-schema.sh"

echo ""
echo "=========================================="
echo "Setup Complete!"
echo "=========================================="

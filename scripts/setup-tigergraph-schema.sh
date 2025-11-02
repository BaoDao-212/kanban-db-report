#!/bin/bash

# TigerGraph Schema Setup Script

set -e

echo "=========================================="
echo "TigerGraph Schema Setup"
echo "=========================================="
echo ""

# Check if container is running
if ! docker ps | grep tigergraph-graph > /dev/null; then
    echo "ERROR: TigerGraph container is not running"
    echo "Please run: ./scripts/setup-databases.sh"
    exit 1
fi

echo "Waiting for TigerGraph to be fully ready..."
sleep 5

echo "Setting up schema..."
docker exec -it tigergraph-graph gsql /tmp/setup.gsql

echo ""
echo "✓ TigerGraph schema setup complete!"
echo ""
echo "Graph 'MyGraph' has been created with:"
echo "  - Vertex: CiNode (id: STRING)"
echo "  - Edge: RELATES_TO (relationTypeId: INT)"
echo ""
echo "Installed queries:"
echo "  - countNodes"
echo "  - countRelationships"
echo "  - getNodesByRelationType"

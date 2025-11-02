# API Examples - Bulk Insert

## 🚀 Quick Examples

### 1. Khởi tạo 50K Nodes + 200K Relationships (All-in-One)

```bash
# Neo4j
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000"

# TigerGraph  
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000"
```

**Response:**
```json
{
  "database": "Neo4j",
  "operation": "BULK_INSERT_LARGE_DATASET",
  "executionTimeMs": 45320,
  "recordCount": 250000,
  "additionalInfo": "Created 50000 nodes (1103.75 nodes/sec) and 200000 relationships (4414.99 rels/sec) in 45.32 seconds. Final counts: 50000 nodes, 200000 relationships"
}
```

### 2. Chỉ tạo 50K Nodes

```bash
curl -X POST "http://localhost:8080/api/bulk/insert-nodes-only?nodeCount=50000"
```

**Response:**
```json
{
  "database": "Neo4j",
  "operation": "BULK_INSERT_NODES",
  "executionTimeMs": 28450,
  "recordCount": 50000,
  "additionalInfo": "Created 50000 nodes in 28.45 seconds (1757.47 nodes/sec). Final count: 50000"
}
```

### 3. Chỉ tạo 200K Relationships (cần có nodes trước)

```bash
# Phải có ít nhất 2 nodes trong database
curl -X POST "http://localhost:8080/api/bulk/insert-relationships-only?relationshipCount=200000"
```

**Response:**
```json
{
  "database": "Neo4j",
  "operation": "BULK_INSERT_RELATIONSHIPS",
  "executionTimeMs": 42180,
  "recordCount": 200000,
  "additionalInfo": "Created 200000 relationships in 42.18 seconds (4741.85 rels/sec). Final count: 200000"
}
```

### 4. Xem thống kê hiện tại

```bash
curl http://localhost:8080/api/bulk/stats
```

**Response:**
```
Database: Neo4j
Nodes: 50,000
Relationships: 200,000
Total Records: 250,000
```

### 5. Xóa toàn bộ dữ liệu

```bash
curl -X DELETE "http://localhost:8080/api/bulk/clear-all"
```

**Response:**
```
All data cleared in 3450 ms using Neo4j
```

## 📊 Advanced Examples

### Test với số lượng nhỏ hơn

```bash
# Test với 10K nodes + 40K relationships
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=10000&relationshipCount=40000"
```

### Test chỉ nodes (nhiều hơn)

```bash
# Test với 100K nodes
curl -X POST "http://localhost:8080/api/bulk/insert-nodes-only?nodeCount=100000"
```

### Test pipeline: Create -> Stats -> Clear

```bash
# 1. Create data
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000" \
  | jq '.'

# 2. Check stats
echo "Statistics:"
curl http://localhost:8080/api/bulk/stats

# 3. Clear all
echo "Clearing..."
curl -X DELETE "http://localhost:8080/api/bulk/clear-all"

# 4. Verify empty
echo "After clear:"
curl http://localhost:8080/api/bulk/stats
```

## 🔄 Comparison Script

```bash
#!/bin/bash

# Function to test a database
test_database() {
    local profile=$1
    local name=$2
    
    echo "Testing $name..."
    
    # Start app
    java -jar target/graph-performance-comparison-1.0.0.jar \
      --spring.profiles.active=$profile > ${profile}.log 2>&1 &
    local pid=$!
    
    # Wait for startup
    sleep 15
    
    # Test
    curl -s -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000" \
      > ${profile}-result.json
    
    # Stats
    echo "$name Stats:"
    curl -s http://localhost:8080/api/bulk/stats
    echo ""
    
    # Cleanup
    kill $pid
    sleep 5
}

# Run tests
test_database "neo4j" "Neo4j"
test_database "tigergraph" "TigerGraph"

# Compare
echo "Comparison:"
echo "Neo4j: $(cat neo4j-result.json | jq -r '.executionTimeMs') ms"
echo "TigerGraph: $(cat tigergraph-result.json | jq -r '.executionTimeMs') ms"
```

## 💡 Tips

### Monitor Progress in Real-time

```bash
# In one terminal, start the app
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=neo4j

# In another terminal, run the bulk insert
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000"

# Watch logs in first terminal for progress:
# "Creating node batch 10/50 (1000 nodes)..."
# "Creating relationship batch 50/200 (1000 relationships)..."
```

### Monitor Docker Resources

```bash
# In separate terminal
watch -n 2 'docker stats --no-stream neo4j-graph tigergraph-graph'
```

### Using jq for Pretty Output

```bash
# Install jq if not available
# Ubuntu/Debian: sudo apt-get install jq
# Mac: brew install jq

# Pretty print response
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000" \
  | jq '.'

# Extract specific fields
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000" \
  | jq '{database, executionTimeMs, recordCount}'
```

### Error Handling

```bash
# Save response and check for errors
RESPONSE=$(curl -s -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000")

# Check if successful
if echo "$RESPONSE" | jq -e '.executionTimeMs > 0' > /dev/null; then
    echo "Success!"
    echo "$RESPONSE" | jq '.'
else
    echo "Failed!"
    echo "$RESPONSE" | jq '.additionalInfo'
fi
```

## 🧪 Testing Different Scenarios

### Scenario 1: Small Dataset (Fast test)
```bash
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=1000&relationshipCount=5000"
```

### Scenario 2: Medium Dataset
```bash
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=10000&relationshipCount=50000"
```

### Scenario 3: Large Dataset (Target)
```bash
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000"
```

### Scenario 4: Extra Large Dataset
```bash
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=100000&relationshipCount=500000"
```

### Scenario 5: Relationships Heavy
```bash
# Few nodes, many relationships (dense graph)
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=5000&relationshipCount=200000"
```

### Scenario 6: Nodes Heavy
```bash
# Many nodes, few relationships (sparse graph)
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=10000"
```

## 📈 Performance Comparison Template

```bash
#!/bin/bash

echo "Database Performance Comparison"
echo "================================"
echo ""

# Test configurations
CONFIGS=(
    "1000:5000:Small"
    "10000:50000:Medium"
    "50000:200000:Large"
)

for config in "${CONFIGS[@]}"; do
    IFS=':' read -r nodes rels label <<< "$config"
    
    echo "Testing $label: $nodes nodes, $rels relationships"
    
    # Neo4j
    echo "  Neo4j..."
    START=$(date +%s)
    curl -s -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=$nodes&relationshipCount=$rels" > /dev/null
    END=$(date +%s)
    NEO4J_TIME=$((END - START))
    
    # Clear
    curl -s -X DELETE "http://localhost:8080/api/bulk/clear-all" > /dev/null
    
    echo "  Neo4j completed in ${NEO4J_TIME}s"
    echo ""
done

echo "================================"
echo "Comparison complete!"
```

## 🎯 One-Liners

```bash
# Quick test
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000" | jq '{db:.database, time:.executionTimeMs, info:.additionalInfo}'

# Test and measure with time command
time curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000"

# Test, save result, show stats
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000" | tee result.json && curl http://localhost:8080/api/bulk/stats

# Clear and verify
curl -X DELETE "http://localhost:8080/api/bulk/clear-all" && curl http://localhost:8080/api/bulk/stats

# Full pipeline one-liner
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000" && curl http://localhost:8080/api/bulk/stats && curl -X DELETE "http://localhost:8080/api/bulk/clear-all"
```

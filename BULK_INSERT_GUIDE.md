# Hướng Dẫn Bulk Insert 50K Nodes + 200K Relationships

## 🚀 Quick Start

### 1. Khởi động databases với Docker Compose

```bash
# Khởi động cả Neo4j và TigerGraph
docker-compose up -d

# Kiểm tra status
docker-compose ps

# Xem logs
docker-compose logs -f
```

### 2. Setup TigerGraph Schema

```bash
# Vào container TigerGraph
docker exec -it tigergraph-graph bash

# Chạy GSQL
gsql

# Tạo schema
USE GLOBAL
DROP ALL

CREATE VERTEX CiNode (PRIMARY_ID id STRING, id STRING) WITH STATS="OUTDEGREE_BY_EDGETYPE"
CREATE DIRECTED EDGE RELATES_TO (FROM CiNode, TO CiNode, relationTypeId INT) WITH REVERSE_EDGE="REVERSE_RELATES_TO"
CREATE GRAPH MyGraph (CiNode, RELATES_TO)

# Exit
exit
exit
```

### 3. Chạy Application

```bash
# Build project
mvn clean package -DskipTests

# Chạy với Neo4j
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=neo4j

# HOẶC chạy với TigerGraph
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=tigergraph
```

---

## 📊 API Endpoints

### 1. Bulk Insert 50K Nodes + 200K Relationships (Tối ưu nhất)

```bash
# Neo4j
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000"

# TigerGraph  
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000"
```

**Đặc điểm:**
- Xử lý theo batch 1000 records/lần
- Tự động xóa data cũ trước khi insert
- Garbage collection giữa các batch
- Hiển thị progress log real-time
- Verify data sau khi insert

**Response Example:**
```json
{
  "database": "Neo4j",
  "operation": "BULK_INSERT_LARGE_DATASET",
  "executionTimeMs": 45320,
  "recordCount": 250000,
  "additionalInfo": "Created 50000 nodes (1103.75 nodes/sec) and 200000 relationships (4414.99 rels/sec) in 45.32 seconds. Final counts: 50000 nodes, 200000 relationships"
}
```

### 2. Bulk Insert Chỉ Nodes

```bash
curl -X POST "http://localhost:8080/api/bulk/insert-nodes-only?nodeCount=50000"
```

### 3. Bulk Insert Chỉ Relationships

```bash
# Phải có nodes trong DB trước
curl -X POST "http://localhost:8080/api/bulk/insert-relationships-only?relationshipCount=200000"
```

### 4. Xem Thống Kê

```bash
curl -X GET "http://localhost:8080/api/bulk/stats"
```

**Response:**
```
Database: Neo4j
Nodes: 50,000
Relationships: 200,000
Total Records: 250,000
```

### 5. Xóa Tất Cả Data

```bash
curl -X DELETE "http://localhost:8080/api/bulk/clear-all"
```

---

## ⚡ Tối Ưu Hóa

### 1. Memory Management

#### Docker Compose Settings:
- **Neo4j**: 6GB limit (4GB heap, 2GB pagecache)
- **TigerGraph**: 6GB limit
- Application tự động:
  - Process theo batch 1000 records
  - Clear batch lists sau mỗi batch
  - GC hint mỗi 10 batches

#### JVM Settings (Optional):
```bash
# Nếu cần tối ưu thêm cho application
java -Xms2G -Xmx4G -XX:+UseG1GC \
  -jar target/graph-performance-comparison-1.0.0.jar \
  --spring.profiles.active=neo4j
```

### 2. Batch Processing Strategy

```
Total: 50,000 nodes
├── Batch 1: nodes 0-999 (1000 nodes)
├── Batch 2: nodes 1000-1999 (1000 nodes)
├── ...
└── Batch 50: nodes 49000-49999 (1000 nodes)

[GC hint every 10 batches]

Total: 200,000 relationships
├── Batch 1: rels 0-999 (1000 rels)
├── Batch 2: rels 1000-1999 (1000 rels)
├── ...
└── Batch 200: rels 199000-199999 (1000 rels)
```

### 3. Database-Specific Optimizations

#### Neo4j:
- Sử dụng `saveAll()` với batch
- Node caching để tránh duplicate queries
- Single transaction per batch

#### TigerGraph:
- REST API batch endpoint
- Bulk upsert vertices và edges
- Parallel processing trên server side

---

## 📈 Performance Benchmarks

### Neo4j (Expected)
- **Nodes**: ~1,000-2,000 nodes/second
- **Relationships**: ~3,000-5,000 rels/second
- **Total Time**: 40-60 seconds cho 50K nodes + 200K rels

### TigerGraph (Expected)
- **Nodes**: ~2,000-5,000 nodes/second
- **Relationships**: ~5,000-10,000 rels/second
- **Total Time**: 20-40 seconds cho 50K nodes + 200K rels

### System Requirements
- **RAM**: Minimum 8GB (recommended 16GB)
- **CPU**: 4+ cores
- **Disk**: 10GB free space
- **Docker**: 12GB RAM allocated

---

## 🔍 Monitoring & Troubleshooting

### 1. Xem Application Logs

```bash
# Real-time progress
tail -f application.log

# Log output shows:
# - Batch progress (e.g., "Creating node batch 10/50...")
# - Performance metrics
# - Error messages
```

### 2. Xem Database Logs

```bash
# Neo4j logs
docker logs neo4j-graph -f

# TigerGraph logs
docker logs tigergraph-graph -f
```

### 3. Monitor Memory Usage

```bash
# Docker stats
docker stats neo4j-graph tigergraph-graph

# System memory
free -h
```

### 4. Common Issues

#### Out of Memory Error
```bash
# Giảm batch size trong BulkInsertController.java
private static final int BATCH_SIZE = 500; // Thay vì 1000
```

#### Connection Timeout
```bash
# Tăng timeout trong application.yml
spring:
  neo4j:
    connection-timeout: 120s
```

#### TigerGraph Not Ready
```bash
# Chờ TigerGraph khởi động hoàn toàn (2-3 phút)
docker exec -it tigergraph-graph gadmin status

# Nếu services chưa up, restart:
docker exec -it tigergraph-graph gadmin start all
```

---

## 🧪 Testing Workflow

### Full Comparison Test

```bash
#!/bin/bash

echo "=== Starting Bulk Insert Comparison ==="

# 1. Test Neo4j
echo "Testing Neo4j..."
# Khởi động app với Neo4j profile
java -jar target/graph-performance-comparison-1.0.0.jar \
  --spring.profiles.active=neo4j > neo4j.log 2>&1 &
APP_PID=$!
sleep 15

curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000" \
  -o neo4j-result.json

echo "Neo4j Stats:"
curl -X GET "http://localhost:8080/api/bulk/stats"

kill $APP_PID
sleep 5

# 2. Test TigerGraph
echo ""
echo "Testing TigerGraph..."
java -jar target/graph-performance-comparison-1.0.0.jar \
  --spring.profiles.active=tigergraph > tigergraph.log 2>&1 &
APP_PID=$!
sleep 15

curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000" \
  -o tigergraph-result.json

echo "TigerGraph Stats:"
curl -X GET "http://localhost:8080/api/bulk/stats"

kill $APP_PID

echo ""
echo "=== Comparison Complete ==="
echo "Results saved to neo4j-result.json and tigergraph-result.json"
```

---

## 🎯 Best Practices

1. **Luôn clear data cũ trước**: API tự động làm điều này
2. **Monitor memory**: Dùng `docker stats` trong quá trình chạy
3. **Batch size**: 1000 là optimal, giảm nếu gặp memory issues
4. **Verify counts**: API tự động verify sau khi insert
5. **Logs**: Luôn check logs để biết progress

## 📝 Configuration Files

### application.yml (Neo4j)
```yaml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: password

graph:
  database:
    type: neo4j
```

### application-tigergraph.yml
```yaml
graph:
  database:
    type: tigergraph

tigergraph:
  host: localhost
  port: 9000
  graph: MyGraph
  username: tigergraph
  password: tigergraph
```

---

## 🆘 Support

Nếu gặp vấn đề:
1. Check logs: application, Docker containers
2. Verify databases đang chạy: `docker ps`
3. Verify schemas: Neo4j Browser, TigerGraph GraphStudio
4. Check memory: `docker stats`, `free -h`

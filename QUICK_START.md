# Quick Start Guide

Hướng dẫn nhanh để bắt đầu với Graph Performance Comparison.

## 1. Cài đặt nhanh (Docker)

```bash
# Clone repository (nếu chưa có)
git clone <repository-url>
cd graph-performance-comparison

# Start databases
./scripts/setup-databases.sh

# Đợi 2-3 phút cho TigerGraph khởi động hoàn toàn

# Setup TigerGraph schema
./scripts/setup-tigergraph-schema.sh

# Build application
mvn clean package
```

## 2. Chạy với Neo4j

```bash
# Start application
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=neo4j

# Hoặc
mvn spring-boot:run -Dspring-boot.run.profiles=neo4j
```

## 3. Chạy với TigerGraph

```bash
# Start application
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=tigergraph

# Hoặc
mvn spring-boot:run -Dspring-boot.run.profiles=tigergraph
```

## 4. Test API cơ bản

```bash
# Kiểm tra database đang dùng
curl http://localhost:8080/api/graph/database-type

# Tạo nodes
curl -X POST http://localhost:8080/api/graph/nodes \
  -H "Content-Type: application/json" \
  -d '{"id": "node-1"}'

curl -X POST http://localhost:8080/api/graph/nodes \
  -H "Content-Type: application/json" \
  -d '{"id": "node-2"}'

# Tạo relationship
curl -X POST http://localhost:8080/api/graph/relationships \
  -H "Content-Type: application/json" \
  -d '{"sourceId": "node-1", "targetId": "node-2", "relationTypeId": 1}'

# Lấy node
curl http://localhost:8080/api/graph/nodes/node-1

# Đếm nodes và relationships
curl http://localhost:8080/api/graph/stats/nodes/count
curl http://localhost:8080/api/graph/stats/relationships/count
```

## 5. Performance Testing

```bash
# Test tạo 1000 nodes
curl -X POST "http://localhost:8080/api/performance/test/create-nodes?count=1000"

# Test tạo 1000 relationships
curl -X POST "http://localhost:8080/api/performance/test/create-relationships?count=1000"

# Test đọc all nodes
curl http://localhost:8080/api/performance/test/read-all-nodes

# Full test suite
curl -X POST "http://localhost:8080/api/performance/test/full-suite?nodeCount=5000&relationshipCount=2500"
```

## 6. So sánh Performance tự động

```bash
# Build trước
mvn clean package

# Chạy comparison script
./scripts/compare-performance.sh 5000 2500

# Xem kết quả
cat neo4j-results.json | jq '.'
cat tigergraph-results.json | jq '.'
```

## 7. Thống kê

```bash
curl http://localhost:8080/api/performance/stats
```

Output:
```
Database: Neo4j
Nodes: 5000
Relationships: 2500
```

## 8. Cleanup

```bash
# Xóa tất cả nodes
curl -X DELETE http://localhost:8080/api/graph/nodes

# Stop databases
docker-compose down

# Remove volumes (xóa data)
docker-compose down -v
```

## API Endpoints Chính

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/graph/nodes` | Tạo node |
| POST | `/api/graph/relationships` | Tạo relationship |
| GET | `/api/graph/nodes/{id}` | Lấy node theo ID |
| GET | `/api/graph/nodes` | Lấy tất cả nodes |
| DELETE | `/api/graph/nodes/{id}` | Xóa node |
| DELETE | `/api/graph/nodes` | Xóa tất cả nodes |
| GET | `/api/graph/stats/nodes/count` | Đếm nodes |
| GET | `/api/graph/stats/relationships/count` | Đếm relationships |
| POST | `/api/performance/test/create-nodes` | Test tạo nodes |
| POST | `/api/performance/test/create-relationships` | Test tạo relationships |
| GET | `/api/performance/test/read-all-nodes` | Test đọc nodes |
| POST | `/api/performance/test/full-suite` | Full performance test |

## Troubleshooting

### Neo4j không connect được
```bash
# Check container
docker ps | grep neo4j

# Check logs
docker logs neo4j-graph

# Restart
docker-compose restart neo4j
```

### TigerGraph không connect được
```bash
# Check container
docker ps | grep tigergraph

# TigerGraph cần 2-3 phút để start
# Check logs
docker logs tigergraph-graph

# Restart
docker-compose restart tigergraph

# Setup schema lại
./scripts/setup-tigergraph-schema.sh
```

### Application không start
```bash
# Check port 8080
lsof -i :8080

# Check logs
tail -f neo4j.log
tail -f tigergraph.log

# Check Java version
java -version  # Cần Java 17+
```

## Performance Tips

1. **Warm up**: Chạy test nhỏ trước để warm up database
2. **Multiple runs**: Chạy test nhiều lần và lấy trung bình
3. **Data cleanup**: Xóa data giữa các test để consistency
4. **Monitor resources**: Check CPU/Memory khi test

## Next Steps

- Đọc [README.md](README.md) để biết chi tiết
- Xem TigerGraph GraphStudio: http://localhost:14240
- Xem Neo4j Browser: http://localhost:7474
- Tùy chỉnh test parameters trong scripts

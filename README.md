# Graph Performance Comparison: Neo4j vs TigerGraph

Ứng dụng Spring Boot để so sánh hiệu suất giữa Neo4j và TigerGraph cho mô hình graph đơn giản với CiNode và relationships.

## Mô hình dữ liệu

### CiNode
- **id**: String - Identifier duy nhất
- **outgoingRelations**: Set<CiRelationship> - Các quan hệ đi ra

### CiRelationship (Relationship Properties)
- **id**: Long - Auto-generated relationship ID
- **relationTypeId**: Long - ID của loại quan hệ
- **target**: CiNode - Node đích

### Relationship Type
- Type: `RELATES_TO` (directed, outgoing)

## Cấu trúc project

```
src/main/java/com/example/graph/
├── GraphPerformanceApplication.java
├── domain/
│   ├── CiNode.java              # Entity chính (Neo4j annotations)
│   └── CiRelationship.java      # Relationship properties
├── dto/
│   ├── CreateNodeRequest.java
│   ├── CreateRelationshipRequest.java
│   └── PerformanceResult.java
├── service/
│   ├── GraphService.java        # Interface chung
│   └── impl/
│       ├── Neo4jGraphService.java      # Implementation cho Neo4j
│       └── TigerGraphService.java      # Implementation cho TigerGraph
├── repository/
│   └── CiNodeRepository.java    # Neo4j repository
├── client/
│   └── TigerGraphClient.java    # REST client cho TigerGraph
├── config/
│   └── TigerGraphConfig.java    # Configuration cho TigerGraph
└── controller/
    ├── GraphController.java     # CRUD operations
    └── PerformanceTestController.java  # Performance testing endpoints
```

## Yêu cầu

- Java 17+
- Maven 3.6+
- Neo4j 5.x (khi dùng Neo4j)
- TigerGraph 3.x+ (khi dùng TigerGraph)

## Cài đặt và chạy

### 1. Cài đặt Neo4j

```bash
# Docker
docker run -d \
  --name neo4j \
  -p 7474:7474 -p 7687:7687 \
  -e NEO4J_AUTH=neo4j/password \
  neo4j:5.14

# Truy cập Neo4j Browser: http://localhost:7474
```

### 2. Cài đặt TigerGraph

```bash
# Docker
docker run -d \
  --name tigergraph \
  -p 9000:9000 -p 14240:14240 \
  docker.tigergraph.com/tigergraph:latest

# Đợi vài phút để TigerGraph khởi động hoàn toàn
docker exec -it tigergraph bash

# Trong container, tạo graph schema
gsql
> CREATE GRAPH MyGraph()
> USE GRAPH MyGraph

# Tạo vertex type
> CREATE VERTEX CiNode (PRIMARY_ID id STRING, id STRING) WITH STATS="OUTDEGREE_BY_EDGETYPE"

# Tạo edge type
> CREATE DIRECTED EDGE RELATES_TO (FROM CiNode, TO CiNode, relationTypeId INT) WITH REVERSE_EDGE="REVERSE_RELATES_TO"

# Install schema
> CREATE GRAPH MyGraph (CiNode, RELATES_TO)
> exit
```

### 3. Build và chạy ứng dụng

```bash
# Build
mvn clean package

# Chạy với Neo4j (mặc định)
java -jar target/graph-performance-comparison-1.0.0.jar

# Hoặc
mvn spring-boot:run

# Chạy với Neo4j (explicit)
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=neo4j

# Chạy với TigerGraph
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=tigergraph
```

## Configuration

### application.yml
Mặc định sử dụng Neo4j. Để đổi sang TigerGraph:

```yaml
graph:
  database:
    type: tigergraph  # neo4j hoặc tigergraph
```

### Neo4j Configuration
```yaml
spring.neo4j:
  uri: bolt://localhost:7687
  authentication:
    username: neo4j
    password: password
```

### TigerGraph Configuration
```yaml
tigergraph:
  host: localhost
  port: 9000
  graph: MyGraph
  username: tigergraph
  password: tigergraph
```

## API Endpoints

### CRUD Operations

#### Tạo node
```bash
POST /api/graph/nodes
Content-Type: application/json

{
  "id": "node-001"
}
```

#### Tạo relationship
```bash
POST /api/graph/relationships
Content-Type: application/json

{
  "sourceId": "node-001",
  "targetId": "node-002",
  "relationTypeId": 1
}
```

#### Lấy node theo ID
```bash
GET /api/graph/nodes/{id}
```

#### Lấy tất cả nodes
```bash
GET /api/graph/nodes
```

#### Xóa node
```bash
DELETE /api/graph/nodes/{id}
```

#### Xóa tất cả nodes
```bash
DELETE /api/graph/nodes
```

#### Đếm nodes
```bash
GET /api/graph/stats/nodes/count
```

#### Đếm relationships
```bash
GET /api/graph/stats/relationships/count
```

#### Lấy nodes theo relation type ID
```bash
GET /api/graph/nodes/by-relation-type/{relationTypeId}
```

#### Kiểm tra database đang dùng
```bash
GET /api/graph/database-type
```

### Performance Testing Endpoints

#### Test tạo nodes
```bash
POST /api/performance/test/create-nodes?count=1000
```

#### Test tạo relationships
```bash
POST /api/performance/test/create-relationships?count=1000
```

#### Test đọc tất cả nodes
```bash
GET /api/performance/test/read-all-nodes
```

#### Test đọc theo relation type
```bash
GET /api/performance/test/read-by-relation-type?relationTypeId=1
```

#### Chạy full test suite
```bash
POST /api/performance/test/full-suite?nodeCount=1000&relationshipCount=500
```

#### Xem thống kê
```bash
GET /api/performance/stats
```

## Performance Testing Examples

### Example 1: So sánh tạo nodes

```bash
# Neo4j
curl -X POST "http://localhost:8080/api/performance/test/create-nodes?count=5000"

# Chuyển sang TigerGraph (restart app với profile tigergraph)
curl -X POST "http://localhost:8080/api/performance/test/create-nodes?count=5000"
```

### Example 2: So sánh full suite

```bash
# Neo4j
curl -X POST "http://localhost:8080/api/performance/test/full-suite?nodeCount=10000&relationshipCount=5000"

# TigerGraph
# (restart với profile tigergraph)
curl -X POST "http://localhost:8080/api/performance/test/full-suite?nodeCount=10000&relationshipCount=5000"
```

### Example Response
```json
{
  "database": "Neo4j",
  "operation": "CREATE_NODES",
  "executionTimeMs": 2543,
  "recordCount": 1000,
  "additionalInfo": "Average: 2.54 ms/node"
}
```

## Performance Comparison Script

Tạo file `compare-performance.sh`:

```bash
#!/bin/bash

echo "=== Graph Database Performance Comparison ==="
echo ""

NODE_COUNT=5000
REL_COUNT=2500

echo "Test parameters:"
echo "- Nodes: $NODE_COUNT"
echo "- Relationships: $REL_COUNT"
echo ""

# Test Neo4j
echo "Testing Neo4j..."
echo "Starting Neo4j instance..."
# Chạy app với Neo4j profile
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=neo4j > neo4j.log 2>&1 &
NEO4J_PID=$!
sleep 10

echo "Running Neo4j tests..."
curl -s -X POST "http://localhost:8080/api/performance/test/full-suite?nodeCount=$NODE_COUNT&relationshipCount=$REL_COUNT" \
  > neo4j-results.json

echo "Neo4j results saved to neo4j-results.json"
kill $NEO4J_PID
sleep 5

# Test TigerGraph
echo ""
echo "Testing TigerGraph..."
echo "Starting TigerGraph instance..."
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=tigergraph > tigergraph.log 2>&1 &
TIGER_PID=$!
sleep 10

echo "Running TigerGraph tests..."
curl -s -X POST "http://localhost:8080/api/performance/test/full-suite?nodeCount=$NODE_COUNT&relationshipCount=$REL_COUNT" \
  > tigergraph-results.json

echo "TigerGraph results saved to tigergraph-results.json"
kill $TIGER_PID

echo ""
echo "=== Comparison Complete ==="
echo "Check neo4j-results.json and tigergraph-results.json for detailed results"
```

## Kiến trúc

### Dual Database Support
Ứng dụng sử dụng Strategy Pattern với:
- **GraphService**: Interface chung cho tất cả operations
- **Neo4jGraphService**: Implementation cho Neo4j (sử dụng Spring Data Neo4j)
- **TigerGraphService**: Implementation cho TigerGraph (sử dụng REST API)

### Conditional Bean Loading
Spring Boot tự động load service phù hợp dựa trên configuration:
```java
@ConditionalOnProperty(name = "graph.database.type", havingValue = "neo4j")
@ConditionalOnProperty(name = "graph.database.type", havingValue = "tigergraph")
```

## Lưu ý

1. **Neo4j**: Sử dụng Spring Data Neo4j với OGM mapping hoàn chỉnh
2. **TigerGraph**: Sử dụng REST API, cần setup schema trước
3. **Performance**: TigerGraph thường nhanh hơn với large-scale graphs và complex queries
4. **Development**: Neo4j dễ dàng hơn cho development với Spring Data support
5. **Scaling**: TigerGraph được thiết kế cho distributed processing

## TigerGraph Schema Setup Script

File `tigergraph-setup.gsql`:

```sql
USE GLOBAL

CREATE GRAPH MyGraph()

USE GRAPH MyGraph

CREATE VERTEX CiNode (
  PRIMARY_ID id STRING,
  id STRING
) WITH STATS="OUTDEGREE_BY_EDGETYPE"

CREATE DIRECTED EDGE RELATES_TO (
  FROM CiNode,
  TO CiNode,
  relationTypeId INT
) WITH REVERSE_EDGE="REVERSE_RELATES_TO"

CREATE GRAPH MyGraph (CiNode, RELATES_TO)

# Optional: Create queries for better performance
CREATE QUERY countNodes() FOR GRAPH MyGraph {
  PRINT count(CiNode.*);
}

CREATE QUERY countRelationships() FOR GRAPH MyGraph {
  PRINT count(RELATES_TO.*);
}

CREATE QUERY getNodesByRelationType(INT relTypeId) FOR GRAPH MyGraph {
  Start = {CiNode.*};
  Result = SELECT s
           FROM Start:s -(RELATES_TO:e)- CiNode:t
           WHERE e.relationTypeId == relTypeId;
  PRINT Result;
}

INSTALL QUERY countNodes
INSTALL QUERY countRelationships
INSTALL QUERY getNodesByRelationType
```

Chạy setup:
```bash
gsql tigergraph-setup.gsql
```

## License

MIT

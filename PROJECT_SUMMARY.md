# Project Summary: Graph Performance Comparison

## Mục đích
So sánh hiệu suất giữa Neo4j và TigerGraph với mô hình graph đơn giản: CiNode và relationships.

## Mô hình dữ liệu đã đơn giản hóa

### Trước (đã bỏ)
- CiNode
- CiType (node riêng)
- RelationshipTypeNode (node riêng)

### Hiện tại (simplified)
```java
CiNode {
    String id
    Set<CiRelationship> outgoingRelations
}

CiRelationship {
    Long id (auto-generated)
    Long relationTypeId    // Chỉ lưu ID, không còn node riêng
    CiNode target
}

Relationship type: "RELATES_TO" (directed)
```

## Kiến trúc

### Strategy Pattern
```
GraphService (interface)
    ├── Neo4jGraphService (Spring Data Neo4j)
    └── TigerGraphService (REST API client)
```

### Chuyển đổi database
```yaml
graph:
  database:
    type: neo4j  # hoặc tigergraph
```

Spring Boot tự động load service tương ứng qua `@ConditionalOnProperty`.

## Cấu trúc thư mục

```
graph-performance-comparison/
├── src/main/java/com/example/graph/
│   ├── GraphPerformanceApplication.java
│   ├── domain/              # CiNode, CiRelationship
│   ├── service/             # GraphService interface + implementations
│   ├── repository/          # Neo4j repositories
│   ├── client/              # TigerGraphClient
│   ├── config/              # TigerGraphConfig
│   ├── controller/          # REST controllers
│   └── dto/                 # Request/Response DTOs
├── src/main/resources/
│   ├── application.yml
│   ├── application-neo4j.yml
│   └── application-tigergraph.yml
├── src/test/
├── scripts/                 # Automation scripts
│   ├── setup-databases.sh
│   ├── setup-tigergraph-schema.sh
│   └── compare-performance.sh
├── examples/
│   └── curl-examples.sh
├── docker-compose.yml       # Neo4j + TigerGraph
├── tigergraph-setup.gsql   # TigerGraph schema
├── postman_collection.json
├── pom.xml
├── README.md
├── QUICK_START.md
├── ARCHITECTURE.md
├── COMPARISON_RESULTS.md
├── CHANGELOG.md
└── LICENSE
```

## API Endpoints chính

### CRUD Operations
- `POST /api/graph/nodes` - Tạo node
- `POST /api/graph/relationships` - Tạo relationship
- `GET /api/graph/nodes/{id}` - Lấy node
- `GET /api/graph/nodes` - Lấy tất cả nodes
- `DELETE /api/graph/nodes/{id}` - Xóa node
- `DELETE /api/graph/nodes` - Xóa tất cả
- `GET /api/graph/stats/nodes/count` - Đếm nodes
- `GET /api/graph/stats/relationships/count` - Đếm relationships
- `GET /api/graph/database-type` - Check database hiện tại

### Performance Testing
- `POST /api/performance/test/create-nodes?count=N`
- `POST /api/performance/test/create-relationships?count=N`
- `GET /api/performance/test/read-all-nodes`
- `GET /api/performance/test/read-by-relation-type?relationTypeId=N`
- `POST /api/performance/test/full-suite?nodeCount=N&relationshipCount=M`
- `GET /api/performance/stats`

## Quick Start

### 1. Setup databases
```bash
./scripts/setup-databases.sh
./scripts/setup-tigergraph-schema.sh
```

### 2. Build
```bash
mvn clean package
```

### 3. Chạy với Neo4j
```bash
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=neo4j
```

### 4. Chạy với TigerGraph
```bash
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=tigergraph
```

### 5. Test API
```bash
./examples/curl-examples.sh
```

### 6. So sánh performance tự động
```bash
./scripts/compare-performance.sh 5000 2500
```

## Tech Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Data Neo4j** - Neo4j integration
- **TigerGraph Java Driver** - TigerGraph integration
- **Lombok** - Boilerplate reduction
- **Jackson** - JSON processing
- **Maven** - Build tool
- **Docker Compose** - Database orchestration

## Database Comparison

| Feature | Neo4j | TigerGraph |
|---------|-------|------------|
| Integration | Spring Data (native) | REST API (manual) |
| Query Language | Cypher | GSQL |
| Transactions | ACID | ACID |
| Scaling | Vertical + Cluster | Horizontal MPP |
| Best For | Deep traversals, dev speed | Large scale, analytics |
| Setup Complexity | Low | Medium |
| Learning Curve | Low | Medium |

## Testing Strategy

1. **Unit Tests**: Domain và service logic
2. **Integration Tests**: Controllers với MockMvc
3. **Performance Tests**: Automated via REST endpoints
4. **Comparison**: Script chạy cả hai databases

## Performance Testing

### Metrics tracked
- Execution time (ms)
- Record count
- Average time per operation
- Throughput

### Test scenarios
1. Create N nodes
2. Create M relationships
3. Read all nodes
4. Read by relation type
5. Full suite (combined)

### Results format
```json
{
  "database": "Neo4j",
  "operation": "CREATE_NODES",
  "executionTimeMs": 2543,
  "recordCount": 1000,
  "additionalInfo": "Average: 2.54 ms/node"
}
```

## Configuration

### application.yml
```yaml
graph:
  database:
    type: neo4j  # neo4j or tigergraph

spring.neo4j:
  uri: bolt://localhost:7687
  authentication:
    username: neo4j
    password: password

tigergraph:
  host: localhost
  port: 9000
  graph: MyGraph
```

### Profiles
- `neo4j`: application-neo4j.yml
- `tigergraph`: application-tigergraph.yml

## Docker Services

### Neo4j
- HTTP: http://localhost:7474
- Bolt: bolt://localhost:7687
- Browser: http://localhost:7474

### TigerGraph
- REST API: http://localhost:9000
- GraphStudio: http://localhost:14240

## Key Implementation Details

### Neo4j
```java
@Node
public class CiNode {
    @Id private String id;
    @Relationship(type = "RELATES_TO", direction = OUTGOING)
    private Set<CiRelationship> outgoingRelations;
}

@RelationshipProperties
public class CiRelationship {
    @RelationshipId private Long id;
    private Long relationTypeId;
    @TargetNode private CiNode target;
}
```

### TigerGraph
- Manual REST API calls
- JSON mapping to domain objects
- Pre-created schema via GSQL:
```sql
CREATE VERTEX CiNode (PRIMARY_ID id STRING, id STRING)
CREATE DIRECTED EDGE RELATES_TO (FROM CiNode, TO CiNode, relationTypeId INT)
CREATE GRAPH MyGraph (CiNode, RELATES_TO)
```

## Extension Points

### Add new operation
1. Update `GraphService` interface
2. Implement in `Neo4jGraphService`
3. Implement in `TigerGraphService`
4. Add controller endpoint
5. Add performance test

### Add third database
1. Create new `GraphService` implementation
2. Add `@ConditionalOnProperty`
3. Add configuration
4. Update documentation

## Documentation Files

- **README.md**: Comprehensive guide
- **QUICK_START.md**: Fast onboarding
- **ARCHITECTURE.md**: Technical details
- **COMPARISON_RESULTS.md**: Performance data template
- **CHANGELOG.md**: Version history
- **PROJECT_SUMMARY.md**: This file

## Scripts

- **setup-databases.sh**: Start Docker containers
- **setup-tigergraph-schema.sh**: Create TigerGraph schema
- **compare-performance.sh**: Automated comparison
- **curl-examples.sh**: API examples

## Tools

- **Postman Collection**: Pre-configured API requests
- **Docker Compose**: One-command database setup
- **Maven Wrapper**: Build without Maven installation

## Monitoring

### Application
- Spring Actuator: http://localhost:8080/actuator
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics

### Databases
- Neo4j Browser: http://localhost:7474
- TigerGraph Studio: http://localhost:14240

## Known Limitations

1. **TigerGraph**: `getNodesByRelationTypeId` not fully implemented (needs GSQL query)
2. **Single instance**: No cluster configuration included
3. **Basic operations**: Advanced graph algorithms not included
4. **No caching**: Direct database calls

## Future Enhancements

- Batch operations
- Graph algorithms comparison
- GraphQL API
- Admin UI with graph visualization
- Metrics export to Prometheus
- Third database support (Neptune, ArangoDB)

## Use Cases

### Chọn Neo4j
- Small to medium graphs (< 10M nodes)
- Development speed priority
- Strong ACID requirement
- Team familiar with Spring/SQL
- Single datacenter

### Chọn TigerGraph
- Large graphs (> 100M nodes)
- High write throughput needed
- Complex analytics required
- Horizontal scaling needed
- Multi-datacenter deployment

## License

MIT License - See LICENSE file

## Support

- Issues: GitHub Issues
- Documentation: README.md, QUICK_START.md
- Examples: examples/ directory
- Scripts: scripts/ directory

---

**Version**: 1.0.0  
**Date**: 2024-01  
**Status**: Production Ready

# Architecture Document

## Tổng quan

Ứng dụng này được thiết kế để so sánh performance giữa Neo4j và TigerGraph với cùng một mô hình dữ liệu graph đơn giản.

## Mô hình dữ liệu

### Simplified Model (Current)
```
CiNode {
  id: String
  outgoingRelations: Set<CiRelationship>
}

CiRelationship {
  id: Long (auto-generated)
  relationTypeId: Long
  target: CiNode
}

Relationship Type: RELATES_TO (directed)
```

### Previous Model (Removed)
- CiType node
- RelationshipTypeNode

Các node này đã bị loại bỏ để đơn giản hóa mô hình chỉ còn CiNode với relationships chứa relationTypeId.

## Architecture Pattern: Strategy Pattern

### Interface Layer
```java
public interface GraphService {
    CiNode createNode(String id);
    void createRelationship(String sourceId, String targetId, Long relationTypeId);
    Optional<CiNode> getNode(String id);
    List<CiNode> getAllNodes();
    void deleteNode(String id);
    void deleteAllNodes();
    long countNodes();
    long countRelationships();
    List<CiNode> getNodesByRelationTypeId(Long relationTypeId);
    String getDatabaseType();
}
```

### Implementation 1: Neo4j
```
Neo4jGraphService implements GraphService
  ├── Uses Spring Data Neo4j
  ├── CiNodeRepository (extends Neo4jRepository)
  ├── OGM mapping with annotations
  └── Transaction management via @Transactional
```

**Key Features:**
- Native Spring Data integration
- Automatic relationship handling
- Cypher query support
- Transaction management

### Implementation 2: TigerGraph
```
TigerGraphService implements GraphService
  ├── Uses TigerGraphClient (custom REST client)
  ├── Manual mapping from JSON to domain objects
  ├── Direct REST API calls
  └── Requires pre-created schema
```

**Key Features:**
- REST API based
- Manual vertex/edge management
- GSQL query support (optional)
- Schema must be created separately

## Database Switching Mechanism

### Configuration-based Selection
```yaml
graph:
  database:
    type: neo4j  # or tigergraph
```

### Conditional Bean Loading
```java
@Service
@ConditionalOnProperty(name = "graph.database.type", havingValue = "neo4j")
class Neo4jGraphService implements GraphService { ... }

@Service
@ConditionalOnProperty(name = "graph.database.type", havingValue = "tigergraph")
class TigerGraphService implements GraphService { ... }
```

Spring Boot sẽ chỉ load một implementation dựa trên configuration.

## Component Diagram

```
┌─────────────────────────────────────────┐
│          REST Controllers               │
│  ┌──────────────┐ ┌──────────────────┐ │
│  │ GraphController│ │PerformanceTest  │ │
│  │              │ │   Controller     │ │
│  └──────────────┘ └──────────────────┘ │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│        GraphService Interface           │
└─────────────┬───────────────────────────┘
              │
      ┌───────┴────────┐
      │                │
      ▼                ▼
┌──────────────┐  ┌──────────────────┐
│ Neo4jGraph   │  │ TigerGraph       │
│ Service      │  │ Service          │
└──────┬───────┘  └────────┬─────────┘
       │                   │
       ▼                   ▼
┌──────────────┐  ┌──────────────────┐
│ CiNode       │  │ TigerGraph       │
│ Repository   │  │ Client           │
└──────┬───────┘  └────────┬─────────┘
       │                   │
       ▼                   ▼
┌──────────────┐  ┌──────────────────┐
│   Neo4j DB   │  │  TigerGraph DB   │
└──────────────┘  └──────────────────┘
```

## Data Flow

### Create Node Flow
```
1. HTTP POST /api/graph/nodes
2. GraphController.createNode()
3. GraphService.createNode(id)
4. [Neo4j] Repository.save() OR [TigerGraph] Client.upsertVertex()
5. Database operation
6. Return CiNode
```

### Create Relationship Flow
```
1. HTTP POST /api/graph/relationships
2. GraphController.createRelationship()
3. GraphService.createRelationship(source, target, typeId)
4. [Neo4j] Load source node, add relationship, save
   OR
   [TigerGraph] Client.upsertEdge()
5. Database operation
6. Return success
```

## Performance Testing Flow

```
1. HTTP POST /api/performance/test/full-suite
2. PerformanceTestController.runFullTestSuite()
3. For each test:
   a. Record start time
   b. Execute operations via GraphService
   c. Record end time
   d. Calculate metrics
4. Return List<PerformanceResult>
```

## Neo4j Implementation Details

### Advantages
- Rich Spring Data support
- Automatic relationship handling
- Cypher query language
- Transaction management
- OGM mapping

### Implementation
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

### Queries
```cypher
// Custom query example
MATCH (n:CiNode)-[r:RELATES_TO]->(m:CiNode) 
WHERE r.relationTypeId = $relationTypeId 
RETURN n, collect(r), collect(m)
```

## TigerGraph Implementation Details

### Advantages
- Optimized for large-scale graphs
- MPP (Massively Parallel Processing)
- GSQL for complex analytics
- High throughput

### Implementation
Uses REST API with manual JSON mapping:
```java
// Vertex creation
POST /graph/{graph}/vertices/{vertexType}/{vertexId}

// Edge creation
POST /graph/{graph}/edges/{sourceType}/{sourceId}/{edgeType}/{targetType}/{targetId}

// Query execution
GET /query/{graph}/{queryName}?param=value
```

### Schema Setup (GSQL)
```sql
CREATE VERTEX CiNode (PRIMARY_ID id STRING, id STRING)
CREATE DIRECTED EDGE RELATES_TO (FROM CiNode, TO CiNode, relationTypeId INT)
CREATE GRAPH MyGraph (CiNode, RELATES_TO)
```

## Testing Strategy

### Unit Tests
- Domain model tests
- Service layer tests (mocked repositories)

### Integration Tests
- Controller tests with MockMvc
- Database-specific tests (with test containers)

### Performance Tests
- Automated via REST endpoints
- Metrics: execution time, throughput
- Comparison script for side-by-side testing

## Deployment Considerations

### Docker Compose
- Neo4j: Single container, bolt protocol
- TigerGraph: Single container, REST API
- Application: Can run as separate container or host

### Production
- Neo4j: Cluster mode for HA
- TigerGraph: Distributed cluster
- Load balancing: Frontend proxy
- Monitoring: Spring Actuator + Prometheus

## Extension Points

### Adding More Operations
1. Add method to GraphService interface
2. Implement in both Neo4jGraphService and TigerGraphService
3. Add controller endpoint
4. Add performance test if needed

### Adding Third Database
1. Create new implementation of GraphService
2. Add @ConditionalOnProperty annotation
3. Add configuration properties
4. Update documentation

### Custom Queries
- Neo4j: Add @Query methods to repository
- TigerGraph: Create GSQL queries and call via client

## Performance Characteristics

### Expected Neo4j Performance
- **Strengths**: 
  - Fast traversals
  - Good for deep queries
  - ACID transactions
- **Weaknesses**:
  - Single-server scaling limits
  - Write throughput limited

### Expected TigerGraph Performance
- **Strengths**:
  - Horizontal scaling
  - High write throughput
  - Complex analytics (GSQL)
- **Weaknesses**:
  - Setup complexity
  - Learning curve for GSQL

## Configuration Management

### Profile-based
```bash
# Neo4j
--spring.profiles.active=neo4j

# TigerGraph
--spring.profiles.active=tigergraph
```

### Environment Variables
```bash
SPRING_NEO4J_URI=bolt://neo4j-server:7687
TIGERGRAPH_HOST=tigergraph-server
```

### application.yml
```yaml
graph:
  database:
    type: ${GRAPH_DB_TYPE:neo4j}

spring.neo4j:
  uri: ${NEO4J_URI:bolt://localhost:7687}

tigergraph:
  host: ${TIGERGRAPH_HOST:localhost}
```

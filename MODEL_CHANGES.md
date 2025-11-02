# Model Changes - From Complex to Simplified

## Tóm tắt thay đổi

Đã đơn giản hóa mô hình graph từ 3 loại nodes (CiNode, CiType, RelationshipTypeNode) xuống còn 1 loại node (CiNode) với relationship properties chứa type information.

## Mô hình trước đây (Assumed/Common Pattern)

```java
// 3 loại nodes
@Node
class CiNode {
    @Id Long id;
    @Relationship(type = "HAS_TYPE")
    CiType ciType;  // Reference tới type node
    
    @Relationship(type = "RELATES_TO")
    Set<CiRelationship> outgoingRelations;
}

@Node
class CiType {
    @Id Long id;
    String name;
    String description;
}

@Node
class RelationshipTypeNode {
    @Id Long id;
    String name;
    String description;
}

@RelationshipProperties
class CiRelationship {
    @RelationshipId Long id;
    
    @Relationship(type = "HAS_REL_TYPE")
    RelationshipTypeNode relationType;  // Reference tới type node
    
    @TargetNode
    CiNode target;
}
```

### Vấn đề của mô hình cũ:
1. **Phức tạp**: 3 loại nodes, nhiều relationships
2. **Performance overhead**: Cần join nhiều nodes để get type info
3. **Khó so sánh**: Khó implement pattern này trên TigerGraph
4. **Overkill**: Cho performance testing, không cần metadata phức tạp

## Mô hình hiện tại (Simplified)

```java
// 1 loại node
@Node
public class CiNode {
    @Id
    private String id;  // Changed to String for flexibility
    
    @Relationship(type = "RELATES_TO", direction = OUTGOING)
    @Builder.Default
    private Set<CiRelationship> outgoingRelations = new HashSet<>();
}

// Relationship properties (không phải node)
@RelationshipProperties
public class CiRelationship {
    @RelationshipId
    private Long id;
    
    private Long relationTypeId;  // Chỉ lưu ID, không reference node
    
    @TargetNode
    private CiNode target;
}

// Không còn CiType node
// Không còn RelationshipTypeNode
```

### Ưu điểm của mô hình mới:
1. ✅ **Đơn giản**: Chỉ 1 loại node
2. ✅ **Performance**: Không cần join thêm nodes
3. ✅ **Dễ implement**: Cả Neo4j và TigerGraph đều support tốt
4. ✅ **Đủ dùng**: relationTypeId đủ để phân loại relationships
5. ✅ **Comparable**: Fair comparison giữa databases

## Chi tiết thay đổi

### 1. CiNode Changes

#### Before:
```java
@Node
class CiNode {
    @Id Long id;
    @Relationship(type = "HAS_TYPE")
    CiType ciType;
    @Relationship(type = "RELATES_TO")
    Set<CiRelationship> outgoingRelations;
}
```

#### After:
```java
@Node
public class CiNode {
    @Id
    private String id;  // String thay vì Long
    
    @Relationship(type = "RELATES_TO", direction = OUTGOING)
    @Builder.Default
    private Set<CiRelationship> outgoingRelations = new HashSet<>();
    
    // Không còn ciType reference
}
```

**Thay đổi**:
- ❌ Removed: `CiType ciType` field
- ✅ Changed: `Long id` → `String id` (flexible, human-readable)
- ✅ Added: `@Builder.Default` for outgoingRelations
- ✅ Added: `direction = OUTGOING` explicit

### 2. CiRelationship Changes

#### Before:
```java
@RelationshipProperties
class CiRelationship {
    @RelationshipId Long id;
    @Relationship(type = "HAS_REL_TYPE")
    RelationshipTypeNode relationType;
    @TargetNode
    CiNode target;
}
```

#### After:
```java
@RelationshipProperties
public class CiRelationship {
    @RelationshipId
    private Long id;
    
    private Long relationTypeId;  // Simple ID instead of node reference
    
    @TargetNode
    private CiNode target;
}
```

**Thay đổi**:
- ❌ Removed: `RelationshipTypeNode relationType` field
- ✅ Changed: `RelationshipTypeNode` → `Long relationTypeId`
- ✅ Simplified: Chỉ lưu ID, không navigate tới type node

### 3. Removed Entities

#### CiType (REMOVED)
```java
// This entity no longer exists
@Node
class CiType {
    @Id Long id;
    String name;
    String description;
}
```

**Lý do loại bỏ**:
- Không cần thiết cho performance testing
- Tăng complexity không cần thiết
- Type information có thể lưu bên ngoài database nếu cần
- CiNode có thể dùng naming convention (e.g., "user-1", "server-1")

#### RelationshipTypeNode (REMOVED)
```java
// This entity no longer exists
@Node
class RelationshipTypeNode {
    @Id Long id;
    String name;
    String description;
}
```

**Lý do loại bỏ**:
- relationTypeId đủ để phân biệt types
- Không cần metadata trong performance test
- Type mapping có thể ở application layer
- Ví dụ: 1 = "DEPENDS_ON", 2 = "CONNECTS_TO", etc.

## Database Schema Changes

### Neo4j

#### Before (assumed):
```cypher
// 3 loại nodes
(:CiNode {id: 1})
(:CiType {id: 1, name: "Server"})
(:RelationshipTypeNode {id: 1, name: "DEPENDS_ON"})

// Multiple relationship types
(:CiNode)-[:HAS_TYPE]->(:CiType)
(:CiNode)-[:RELATES_TO {id: 1}]->(:RelationshipTypeNode)
(:CiNode)-[:RELATES_TO]->(:CiNode)
```

#### After:
```cypher
// 1 loại node
(:CiNode {id: "node-1"})
(:CiNode {id: "node-2"})

// 1 relationship type với properties
(:CiNode {id: "node-1"})-[:RELATES_TO {relationTypeId: 1}]->(:CiNode {id: "node-2"})
```

### TigerGraph

#### Before (would be complex):
```sql
CREATE VERTEX CiNode (PRIMARY_ID id INT)
CREATE VERTEX CiType (PRIMARY_ID id INT, name STRING)
CREATE VERTEX RelationshipTypeNode (PRIMARY_ID id INT, name STRING)

CREATE EDGE HAS_TYPE (FROM CiNode, TO CiType)
CREATE EDGE RELATES_TO (FROM CiNode, TO CiNode)
CREATE EDGE HAS_REL_TYPE (FROM RELATES_TO, TO RelationshipTypeNode)  -- Complex!
```

#### After (simple):
```sql
CREATE VERTEX CiNode (PRIMARY_ID id STRING, id STRING)

CREATE DIRECTED EDGE RELATES_TO (
    FROM CiNode, 
    TO CiNode, 
    relationTypeId INT
)
```

## Impact on Operations

### Create Node

#### Before:
```java
// Cần create hoặc reference CiType
CiType type = ciTypeRepository.findById(typeId);
CiNode node = new CiNode();
node.setCiType(type);
ciNodeRepository.save(node);
```

#### After:
```java
// Chỉ cần ID
CiNode node = CiNode.builder()
    .id("node-1")
    .build();
ciNodeRepository.save(node);
```

### Create Relationship

#### Before:
```java
// Cần load type node
RelationshipTypeNode relType = relationTypeRepository.findById(typeId);
CiRelationship rel = new CiRelationship();
rel.setRelationType(relType);
rel.setTarget(targetNode);
// ...
```

#### After:
```java
// Chỉ cần ID
CiRelationship rel = CiRelationship.builder()
    .relationTypeId(1L)
    .target(targetNode)
    .build();
```

### Query by Type

#### Before:
```cypher
// Complex query with multiple hops
MATCH (n:CiNode)-[:HAS_TYPE]->(t:CiType {name: "Server"})
RETURN n
```

#### After:
```cypher
// Simple - hoặc query by convention
MATCH (n:CiNode)
WHERE n.id STARTS WITH 'server-'
RETURN n

// Hoặc by relationship type
MATCH (n:CiNode)-[r:RELATES_TO]->(m)
WHERE r.relationTypeId = 1
RETURN n
```

## Type Management

### Application-Level Type Mapping

Vì không còn type nodes, có thể manage types ở application layer:

```java
// Enum for CI types (optional)
public enum CiTypeEnum {
    SERVER(1, "server-"),
    DATABASE(2, "db-"),
    APPLICATION(3, "app-");
    
    private final int code;
    private final String prefix;
}

// Enum for relationship types
public enum RelationTypeEnum {
    DEPENDS_ON(1),
    CONNECTS_TO(2),
    USES(3);
    
    private final long id;
}
```

**Sử dụng**:
```java
// Create với convention
String nodeId = CiTypeEnum.SERVER.getPrefix() + UUID.randomUUID();
CiNode server = graphService.createNode(nodeId);

// Create relationship với type
graphService.createRelationship(
    sourceId, 
    targetId, 
    RelationTypeEnum.DEPENDS_ON.getId()
);
```

## Migration Guide (If Needed)

Nếu có data cũ cần migrate:

```cypher
// Neo4j migration
// 1. Update CiNode IDs
MATCH (n:CiNode)-[:HAS_TYPE]->(t:CiType)
SET n.id = toLower(t.name) + '-' + toString(n.id)

// 2. Update relationships
MATCH (n:CiNode)-[r:RELATES_TO]->(m:CiNode)
MATCH (r)-[:HAS_REL_TYPE]->(rt:RelationshipTypeNode)
SET r.relationTypeId = rt.id

// 3. Remove type nodes
MATCH (t:CiType) DETACH DELETE t
MATCH (rt:RelationshipTypeNode) DETACH DELETE rt
```

## Validation

### Ensure model is correct:

```java
// Test CiNode
CiNode node = CiNode.builder()
    .id("test-1")
    .build();
assert node.getOutgoingRelations() != null;  // Default empty set

// Test CiRelationship
CiNode target = CiNode.builder().id("test-2").build();
CiRelationship rel = CiRelationship.builder()
    .relationTypeId(1L)
    .target(target)
    .build();
assert rel.getRelationTypeId() == 1L;
assert rel.getTarget().getId().equals("test-2");
```

## Performance Benefits

### Reduced Query Complexity
- Before: 3-node query = 2 hops
- After: 1-node query = 0 hops (or 1 for relationship)

### Reduced Storage
- Before: 3 nodes per CI item
- After: 1 node per CI item

### Faster Writes
- Before: Multiple node creates/updates
- After: Single node operation

### Better Comparison
- Apples-to-apples comparison between databases
- Same model complexity for both
- Fair performance metrics

## Summary

| Aspect | Before | After |
|--------|--------|-------|
| Node types | 3 (CiNode, CiType, RelationshipTypeNode) | 1 (CiNode) |
| CiNode ID | Long | String |
| Type info | Node references | Simple IDs |
| Complexity | High | Low |
| Query hops | 2-3 | 0-1 |
| Implementation | Complex | Simple |
| Neo4j fit | Good | Excellent |
| TigerGraph fit | Difficult | Excellent |
| Performance | Slower | Faster |

## Conclusion

Mô hình đơn giản hóa:
✅ Dễ implement  
✅ Dễ understand  
✅ Dễ maintain  
✅ Better performance  
✅ Fair comparison  
✅ Flexible (String IDs)  
✅ Production-ready  

Đây là mô hình phù hợp cho:
- Performance testing
- Benchmarking
- Learning graph databases
- Prototyping
- Simple graph use cases

---

**Status**: ✅ Implemented  
**Version**: 1.0.0  
**Date**: 2024-01

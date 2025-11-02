# Performance Comparison Results

Tài liệu này mô tả kết quả so sánh performance dự kiến giữa Neo4j và TigerGraph.

## Test Environment

- **Hardware**: [To be filled]
- **Neo4j Version**: 5.14
- **TigerGraph Version**: 3.x
- **Dataset**: CiNode with RELATES_TO relationships
- **Test Scale**: Various (1K, 5K, 10K, 50K nodes)

## Comparison Metrics

### 1. Create Operations

#### Create Nodes
| Database | 1K Nodes | 5K Nodes | 10K Nodes | Avg ms/node |
|----------|----------|----------|-----------|-------------|
| Neo4j | TBD | TBD | TBD | TBD |
| TigerGraph | TBD | TBD | TBD | TBD |

#### Create Relationships
| Database | 1K Rels | 5K Rels | 10K Rels | Avg ms/rel |
|----------|---------|---------|----------|------------|
| Neo4j | TBD | TBD | TBD | TBD |
| TigerGraph | TBD | TBD | TBD | TBD |

### 2. Read Operations

#### Read All Nodes
| Database | 1K Nodes | 5K Nodes | 10K Nodes | ms/total |
|----------|----------|----------|-----------|----------|
| Neo4j | TBD | TBD | TBD | TBD |
| TigerGraph | TBD | TBD | TBD | TBD |

#### Read by Relation Type
| Database | 1K Nodes | 5K Nodes | 10K Nodes | ms/query |
|----------|----------|----------|-----------|----------|
| Neo4j | TBD | TBD | TBD | TBD |
| TigerGraph | TBD | TBD | TBD | TBD |

### 3. Complex Queries

#### Multi-hop Traversal
| Database | 2 Hops | 3 Hops | 4 Hops | Notes |
|----------|--------|--------|--------|-------|
| Neo4j | TBD | TBD | TBD | Native traversal |
| TigerGraph | TBD | TBD | TBD | Requires GSQL query |

## Expected Results

### Neo4j Strengths
- **Fast deep traversals**: Neo4j được tối ưu cho index-free adjacency
- **ACID transactions**: Strong consistency
- **Developer experience**: Spring Data integration rất tốt
- **Complex queries**: Cypher rất expressive

### TigerGraph Strengths
- **High throughput writes**: MPP architecture
- **Large-scale graphs**: Distributed processing
- **Complex analytics**: GSQL cho advanced analytics
- **Horizontal scaling**: Thêm nodes để tăng capacity

## Use Case Recommendations

### Chọn Neo4j khi:
- Graph size < 10M nodes
- Cần ACID transactions nghiêm ngặt
- Queries chủ yếu là traversals
- Team quen với Spring ecosystem
- Development speed là priority
- Single-server deployment

### Chọn TigerGraph khi:
- Graph size > 100M nodes
- Cần high write throughput
- Complex analytics là core requirement
- Horizontal scaling cần thiết
- Real-time analytics
- Graph algorithms (PageRank, Community Detection, etc.)

## Detailed Test Scenarios

### Scenario 1: Social Network
```
Nodes: 10K users
Relationships: 50K friendships (relationTypeId: 1)
Query: Find friends of friends
```

**Expected:**
- Neo4j: Faster for 2-3 hop queries
- TigerGraph: Better at scale (100K+ users)

### Scenario 2: High Write Load
```
Operation: Continuous node/relationship creation
Throughput: 1000 writes/second
Duration: 10 minutes
```

**Expected:**
- Neo4j: ~800-1000 writes/sec (single instance)
- TigerGraph: ~5000+ writes/sec (distributed)

### Scenario 3: Complex Analytics
```
Query: Community detection across entire graph
Graph: 50K nodes, 200K relationships
```

**Expected:**
- Neo4j: Minutes (using APOC procedures)
- TigerGraph: Seconds (native GSQL algorithms)

## Running Your Own Comparison

### Quick Test (5 minutes)
```bash
# Build
mvn clean package

# Run comparison
./scripts/compare-performance.sh 1000 500

# View results
cat neo4j-results.json | jq '.'
cat tigergraph-results.json | jq '.'
```

### Comprehensive Test (30 minutes)
```bash
# Test multiple scales
for count in 1000 5000 10000 20000; do
  echo "Testing with $count nodes..."
  ./scripts/compare-performance.sh $count $((count / 2))
  mv neo4j-results.json "neo4j-results-${count}.json"
  mv tigergraph-results.json "tigergraph-results-${count}.json"
done

# Analyze results
python analyze-results.py
```

## Analysis Script Example

File: `analyze-results.py`
```python
import json
import matplotlib.pyplot as plt

# Load results
with open('neo4j-results.json') as f:
    neo4j = json.load(f)
    
with open('tigergraph-results.json') as f:
    tigergraph = json.load(f)

# Extract metrics
operations = [r['operation'] for r in neo4j]
neo4j_times = [r['executionTimeMs'] for r in neo4j]
tiger_times = [r['executionTimeMs'] for r in tigergraph]

# Plot comparison
plt.figure(figsize=(10, 6))
x = range(len(operations))
plt.bar([i - 0.2 for i in x], neo4j_times, width=0.4, label='Neo4j')
plt.bar([i + 0.2 for i in x], tiger_times, width=0.4, label='TigerGraph')
plt.xticks(x, operations, rotation=45)
plt.ylabel('Execution Time (ms)')
plt.title('Neo4j vs TigerGraph Performance Comparison')
plt.legend()
plt.tight_layout()
plt.savefig('comparison.png')
print("Chart saved to comparison.png")
```

## Monitoring During Tests

### Neo4j Monitoring
```bash
# Browser
http://localhost:7474

# Metrics
curl http://localhost:7474/db/neo4j/cluster/available
```

### TigerGraph Monitoring
```bash
# GraphStudio
http://localhost:14240

# Metrics
curl http://localhost:9000/statistics/{graph}
```

### Application Monitoring
```bash
# Spring Actuator
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/metrics
```

## Optimization Tips

### Neo4j Optimization
1. Add indexes: `CREATE INDEX ON :CiNode(id)`
2. Tune memory: `NEO4J_dbms_memory_heap_max__size=4G`
3. Use batching for bulk inserts
4. Enable query logging for slow queries

### TigerGraph Optimization
1. Use batch loading (REST++ endpoints)
2. Create custom GSQL queries for complex operations
3. Partition large graphs
4. Use appropriate data types in schema

## Cost Considerations

### Infrastructure Costs
| Database | Single Node | 3-Node Cluster | Enterprise |
|----------|-------------|----------------|------------|
| Neo4j | Free/Aura | $$ | $$$ |
| TigerGraph | Free | $$ | $$$ |

### Development Costs
- Neo4j: Lower (Spring Data integration)
- TigerGraph: Higher (Learning GSQL, REST API)

## Conclusion

Cả hai databases đều có ưu điểm riêng:

**Neo4j**: Best cho development speed, moderate scale, deep traversals
**TigerGraph**: Best cho large scale, high throughput, complex analytics

Recommendation: Bắt đầu với Neo4j để rapid development, chuyển sang TigerGraph khi scale demands it.

---

**Note**: Điền vào bảng TBD bằng cách chạy actual performance tests với `./scripts/compare-performance.sh`

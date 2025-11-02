# Deployment Checklist - Bulk Insert 50K Nodes + 200K Relationships

## ✅ Pre-Deployment Checklist

### 1. System Requirements
- [ ] Docker installed and running
- [ ] At least 12GB RAM available for Docker
- [ ] At least 10GB free disk space
- [ ] Java 17+ installed
- [ ] Maven 3.6+ installed (or use included Maven wrapper)

### 2. Build Application

```bash
# Navigate to project directory
cd /home/engine/project

# Build the application
mvn clean package -DskipTests

# Verify JAR file created
ls -lh target/graph-performance-comparison-1.0.0.jar
```

### 3. Start Databases

```bash
# Start both Neo4j and TigerGraph
docker-compose up -d

# Verify containers are running
docker ps

# Expected output: neo4j-graph and tigergraph-graph containers

# Check Neo4j logs
docker logs neo4j-graph

# Check TigerGraph logs
docker logs tigergraph-graph

# Wait for databases to be healthy (2-3 minutes for TigerGraph)
```

### 4. Setup TigerGraph Schema (First Time Only)

```bash
# Enter TigerGraph container
docker exec -it tigergraph-graph bash

# Wait for services to be ready
gadmin status

# If not all services are running:
gadmin start all

# Setup schema
gsql

# Run these GSQL commands:
USE GLOBAL
DROP ALL

CREATE VERTEX CiNode (PRIMARY_ID id STRING, id STRING) WITH STATS="OUTDEGREE_BY_EDGETYPE"

CREATE DIRECTED EDGE RELATES_TO (FROM CiNode, TO CiNode, relationTypeId INT) WITH REVERSE_EDGE="REVERSE_RELATES_TO"

CREATE GRAPH MyGraph (CiNode, RELATES_TO)

# Exit GSQL
exit

# Exit container
exit
```

### 5. Verify Database Connections

#### Neo4j:
```bash
# Open browser: http://localhost:7474
# Login: neo4j / password
# Run query: MATCH (n) RETURN count(n)
```

#### TigerGraph:
```bash
# Test REST API
curl http://localhost:9000/echo

# Expected: {"error":false,"message":"Hello GSQL"}
```

## 🚀 Deployment Steps

### Option 1: Manual Testing

#### Test Neo4j:
```bash
# 1. Start application with Neo4j profile
java -Xms2G -Xmx4G -XX:+UseG1GC \
  -jar target/graph-performance-comparison-1.0.0.jar \
  --spring.profiles.active=neo4j > neo4j-app.log 2>&1 &

# Save PID
NEO4J_PID=$!
echo $NEO4J_PID

# 2. Wait for application to start (15 seconds)
sleep 15

# 3. Test health
curl http://localhost:8080/actuator/health

# 4. Run bulk insert
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000" \
  | jq '.'

# 5. Check stats
curl http://localhost:8080/api/bulk/stats

# 6. Stop application
kill $NEO4J_PID
```

#### Test TigerGraph:
```bash
# 1. Start application with TigerGraph profile
java -Xms2G -Xmx4G -XX:+UseG1GC \
  -jar target/graph-performance-comparison-1.0.0.jar \
  --spring.profiles.active=tigergraph > tigergraph-app.log 2>&1 &

# Save PID
TIGER_PID=$!
echo $TIGER_PID

# 2. Wait for application to start (15 seconds)
sleep 15

# 3. Test health
curl http://localhost:8080/actuator/health

# 4. Run bulk insert
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000" \
  | jq '.'

# 5. Check stats
curl http://localhost:8080/api/bulk/stats

# 6. Stop application
kill $TIGER_PID
```

### Option 2: Automated Testing Script

```bash
# Make script executable
chmod +x scripts/bulk-insert-test.sh

# Run automated comparison
./scripts/bulk-insert-test.sh

# Results will be saved to:
# - neo4j-result.json
# - tigergraph-result.json
# - neo4j-app.log
# - tigergraph-app.log
```

## 📊 Monitoring During Execution

### Monitor Docker Resources

```bash
# Open new terminal
watch -n 2 'docker stats --no-stream neo4j-graph tigergraph-graph'
```

### Monitor Application Logs

```bash
# For Neo4j
tail -f neo4j-app.log

# For TigerGraph
tail -f tigergraph-app.log

# Look for batch progress messages:
# "Creating node batch 10/50 (1000 nodes)..."
# "Creating relationship batch 50/200 (1000 relationships)..."
```

### Monitor System Resources

```bash
# Memory usage
free -h

# CPU usage
top

# Disk space
df -h
```

## 🎯 Expected Results

### Performance Benchmarks

#### Neo4j:
- **Node Creation**: 1,000-2,000 nodes/second
- **Relationship Creation**: 3,000-5,000 rels/second
- **Total Time**: 40-60 seconds
- **Memory Usage**: 4-5 GB peak

#### TigerGraph:
- **Node Creation**: 2,000-5,000 nodes/second
- **Relationship Creation**: 5,000-10,000 rels/second
- **Total Time**: 20-40 seconds
- **Memory Usage**: 4-5 GB peak

### Success Criteria

- [ ] Application starts without errors
- [ ] All 50,000 nodes created successfully
- [ ] All 200,000 relationships created successfully
- [ ] Final counts match requested counts
- [ ] No OutOfMemory errors
- [ ] No connection timeout errors
- [ ] Response time < 2 minutes

## ⚠️ Troubleshooting

### Issue: OutOfMemoryError

**Solution:**
1. Reduce batch size in `BulkInsertController.java`:
   ```java
   private static final int BATCH_SIZE = 500; // Instead of 1000
   ```
2. Increase Docker memory limits in `docker-compose.yml`
3. Increase JVM heap: `-Xmx6G`

### Issue: Connection Timeout

**Solution:**
1. Check database is running: `docker ps`
2. Check database health: `docker logs [container-name]`
3. Increase timeout in application.yml
4. Restart database container

### Issue: TigerGraph Schema Not Found

**Solution:**
1. Verify schema created: `docker exec -it tigergraph-graph gsql "ls"`
2. Re-run schema setup commands
3. Check GSQL errors: `docker logs tigergraph-graph`

### Issue: Slow Performance

**Possible Causes:**
1. Insufficient memory allocated to Docker
2. Running other heavy applications
3. Disk I/O bottleneck
4. Too many parallel operations

**Solutions:**
1. Close other applications
2. Increase Docker resources
3. Use SSD instead of HDD
4. Run tests one at a time

## 📈 Post-Deployment Validation

### 1. Verify Data Integrity

#### Neo4j:
```cypher
// Open Neo4j Browser: http://localhost:7474

// Count nodes
MATCH (n:CiNode) RETURN count(n)

// Count relationships
MATCH ()-[r:RELATES_TO]->() RETURN count(r)

// Sample data
MATCH (n:CiNode)-[r:RELATES_TO]->(m:CiNode)
RETURN n, r, m
LIMIT 10
```

#### TigerGraph:
```bash
# Via REST API
curl "http://localhost:9000/builtins/MyGraph/stat/vertex_number?type=CiNode"
curl "http://localhost:9000/builtins/MyGraph/stat/edge_number?type=RELATES_TO"
```

### 2. Performance Analysis

```bash
# Compare results
cat neo4j-result.json | jq '.executionTimeMs'
cat tigergraph-result.json | jq '.executionTimeMs'

# Calculate speedup
# (Neo4j time - TigerGraph time) / TigerGraph time * 100
```

### 3. Clean Up (Optional)

```bash
# Stop application
pkill -f graph-performance-comparison

# Clear all data
curl -X DELETE http://localhost:8080/api/bulk/clear-all

# Stop databases
docker-compose down

# Remove volumes (WARNING: deletes all data)
docker-compose down -v
```

## 📝 Notes

- First run may be slower due to database initialization
- Subsequent runs should be faster due to caching
- Results may vary based on system resources
- TigerGraph typically performs better on large-scale operations
- Neo4j may perform better on complex traversal queries

## 🔗 Related Documentation

- [BULK_INSERT_GUIDE.md](BULK_INSERT_GUIDE.md) - Detailed guide
- [README.md](README.md) - Project overview
- [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture details
- [docker-compose.yml](docker-compose.yml) - Docker configuration

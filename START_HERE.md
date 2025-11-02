# 🚀 START HERE - Bulk Insert 50K Nodes + 200K Relationships

## Bạn đang ở đây để làm gì?

Tạo **50,000 nodes** và **200,000 relationships** vào Neo4j và TigerGraph một cách **tối ưu** và **nhanh chóng**.

---

## ⚡ 3 Bước Nhanh Nhất

### Bước 1: Khởi động databases
```bash
docker-compose up -d
```

### Bước 2: Setup TigerGraph (chỉ lần đầu)
```bash
docker exec -it tigergraph-graph bash -c 'gsql "USE GLOBAL; DROP ALL; CREATE VERTEX CiNode (PRIMARY_ID id STRING, id STRING) WITH STATS=\"OUTDEGREE_BY_EDGETYPE\"; CREATE DIRECTED EDGE RELATES_TO (FROM CiNode, TO CiNode, relationTypeId INT); CREATE GRAPH MyGraph (CiNode, RELATES_TO);"'
```

### Bước 3: Chạy test
```bash
# Build
mvn clean package -DskipTests

# Test Neo4j
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=neo4j &
sleep 15
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000"

# Kill và test TigerGraph
pkill -f graph-performance
sleep 5
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=tigergraph &
sleep 15
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000"
```

**Done! 🎉**

---

## 📖 Bạn muốn biết thêm gì?

### 🎯 API nào để dùng?
→ Xem [API_EXAMPLES.md](API_EXAMPLES.md)

### 📚 Hướng dẫn chi tiết?
→ Xem [BULK_INSERT_GUIDE.md](BULK_INSERT_GUIDE.md)

### ✅ Checklist deployment?
→ Xem [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)

### 🔍 Tổng quan implementation?
→ Xem [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)

### 📖 Overview project?
→ Xem [README.md](README.md)

---

## 🎯 API Chính

### 1️⃣ Tạo tất cả (50K nodes + 200K rels)
```bash
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000"
```

### 2️⃣ Xem thống kê
```bash
curl http://localhost:8080/api/bulk/stats
```

### 3️⃣ Xóa tất cả
```bash
curl -X DELETE "http://localhost:8080/api/bulk/clear-all"
```

---

## 💡 Tính năng nổi bật

✅ **Tối ưu RAM**: Batch processing 1000 records/lần, auto GC  
✅ **Nhanh**: 40-60s cho Neo4j, 20-40s cho TigerGraph  
✅ **Progress logging**: Xem tiến độ real-time  
✅ **Auto verify**: Tự động kiểm tra số lượng sau khi insert  
✅ **Error handling**: Xử lý lỗi tốt, message rõ ràng  

---

## 🛠️ Troubleshooting Nhanh

### ❌ Docker container không chạy?
```bash
docker-compose ps
docker-compose logs
docker-compose restart
```

### ❌ OutOfMemory error?
Giảm batch size trong `BulkInsertController.java`:
```java
private static final int BATCH_SIZE = 500; // thay vì 1000
```

### ❌ Application không start?
```bash
# Check port 8080 có bị chiếm không
lsof -i :8080
# Kill process nếu cần
kill -9 [PID]
```

### ❌ TigerGraph schema error?
```bash
docker exec -it tigergraph-graph bash
gadmin start all
gsql "USE GRAPH MyGraph; ls"
```

---

## 📊 Kết quả mong đợi

### Neo4j
- ⏱️ **Thời gian**: 40-60 giây
- 📈 **Tốc độ nodes**: 1,000-2,000 nodes/sec
- 📈 **Tốc độ rels**: 3,000-5,000 rels/sec
- 💾 **Memory**: 4-5 GB peak

### TigerGraph
- ⏱️ **Thời gian**: 20-40 giây
- 📈 **Tốc độ nodes**: 2,000-5,000 nodes/sec
- 📈 **Tốc độ rels**: 5,000-10,000 rels/sec
- 💾 **Memory**: 4-5 GB peak

---

## 🎓 Kiến trúc tóm tắt

```
BulkInsertController
    ↓
GraphService (interface)
    ├── Neo4jGraphService
    │   └── Batch với saveAll() + caching
    └── TigerGraphService
        └── Batch với REST API

Batch Processing:
- 50,000 nodes = 50 batches × 1000 nodes
- 200,000 rels = 200 batches × 1000 rels
- GC hint mỗi 10 batches
```

---

## 📁 File Structure

```
project/
├── START_HERE.md                    ← Bạn đang ở đây!
├── API_EXAMPLES.md                  ← Practical examples
├── BULK_INSERT_GUIDE.md             ← Complete guide
├── DEPLOYMENT_CHECKLIST.md          ← Step-by-step deployment
├── IMPLEMENTATION_SUMMARY.md        ← Technical details
├── docker-compose.yml               ← Optimized config
├── scripts/
│   └── bulk-insert-test.sh          ← Automated testing
└── src/main/java/.../controller/
    └── BulkInsertController.java    ← Main API
```

---

## 🚦 Workflow Đề Xuất

### Development:
1. Start databases: `docker-compose up -d`
2. Setup TigerGraph schema (first time)
3. Build: `mvn clean package -DskipTests`
4. Run with profile: `--spring.profiles.active=neo4j` or `tigergraph`
5. Test API với Postman hoặc curl

### Testing:
1. Use `scripts/bulk-insert-test.sh` for automated tests
2. Monitor logs for progress
3. Check stats API for verification
4. Compare Neo4j vs TigerGraph results

### Production:
1. Follow [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)
2. Adjust memory limits if needed
3. Monitor with Docker stats
4. Use health checks

---

## 🎯 Quick Commands Reference

```bash
# Start everything
docker-compose up -d

# Build
mvn clean package -DskipTests

# Run Neo4j
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=neo4j &

# Run TigerGraph  
java -jar target/graph-performance-comparison-1.0.0.jar --spring.profiles.active=tigergraph &

# Bulk insert
curl -X POST "http://localhost:8080/api/bulk/insert-large-dataset?nodeCount=50000&relationshipCount=200000"

# Stats
curl http://localhost:8080/api/bulk/stats

# Clear
curl -X DELETE "http://localhost:8080/api/bulk/clear-all"

# Stop app
pkill -f graph-performance

# Stop databases
docker-compose down
```

---

## ✅ Đã Sẵn Sàng!

Tất cả đã được setup và optimize. Chỉ cần:
1. ✅ Docker Compose với memory optimization
2. ✅ 2 API bulk insert tối ưu
3. ✅ Batch processing để tránh RAM full
4. ✅ Complete documentation
5. ✅ Testing scripts

**Chúc bạn test thành công! 🚀**

---

## 📞 Need Help?

- **API Examples**: [API_EXAMPLES.md](API_EXAMPLES.md)
- **Full Guide**: [BULK_INSERT_GUIDE.md](BULK_INSERT_GUIDE.md)  
- **Deployment**: [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)
- **Technical**: [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md)

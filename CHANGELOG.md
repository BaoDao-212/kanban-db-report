# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2024-01-XX

### Added
- Initial implementation of Graph Performance Comparison system
- Dual database support: Neo4j and TigerGraph
- Domain model with CiNode and CiRelationship
- Simplified model: removed CiType and RelationshipTypeNode
- GraphService interface with two implementations:
  - Neo4jGraphService using Spring Data Neo4j
  - TigerGraphService using REST API
- CRUD operations for nodes and relationships
- Performance testing endpoints
- Docker Compose setup for databases
- Automated performance comparison script
- Comprehensive documentation

### Features
- Create/Read/Delete operations for CiNode
- Create relationships with relationTypeId
- Count nodes and relationships
- Query nodes by relation type ID
- Performance metrics tracking
- RESTful API with JSON responses
- Profile-based database selection
- Postman collection for API testing

### Documentation
- README.md with comprehensive setup guide
- QUICK_START.md for rapid onboarding
- ARCHITECTURE.md with technical details
- COMPARISON_RESULTS.md template for performance data
- Example scripts and curl commands

### Technical Details
- Spring Boot 3.2.0
- Java 17
- Neo4j 5.14 support
- TigerGraph 3.x support
- Lombok for boilerplate reduction
- Jackson for JSON processing
- Maven build system

### Configuration
- application.yml for default configuration
- application-neo4j.yml profile
- application-tigergraph.yml profile
- Environment variable support
- Docker Compose configuration

### Testing
- Spring Boot test setup
- Example curl scripts
- Postman collection
- Performance comparison automation

## Model Changes

### Removed (from previous design)
- CiType node entity
- RelationshipTypeNode entity
- Complex type hierarchies

### Current Model
```
CiNode:
  - id: String
  - outgoingRelations: Set<CiRelationship>

CiRelationship:
  - id: Long (auto-generated)
  - relationTypeId: Long
  - target: CiNode
  
Relationship Type: "RELATES_TO" (directed)
```

### Rationale
The model was simplified to focus on performance comparison rather than complex type management. The relationTypeId in CiRelationship provides sufficient typing without separate type nodes.

## Known Limitations

### TigerGraph
- getNodesByRelationTypeId requires custom GSQL query (not implemented)
- Manual schema setup required
- No automatic OGM mapping

### Neo4j
- Single-server performance limits
- Requires transaction management

## Future Enhancements

### Planned
- [ ] Batch operations API
- [ ] Custom GSQL queries for TigerGraph
- [ ] More complex traversal patterns
- [ ] Graph algorithms comparison
- [ ] Metrics export to Prometheus
- [ ] Async operations support
- [ ] GraphQL API layer

### Under Consideration
- [ ] Third database support (e.g., Amazon Neptune, ArangoDB)
- [ ] WebSocket for real-time updates
- [ ] Admin UI for graph visualization
- [ ] Performance benchmarking framework
- [ ] Load testing suite

## Migration Guide

Not applicable for initial release.

## Contributors

- Initial implementation: [Your Team]

## License

MIT License - See LICENSE file for details

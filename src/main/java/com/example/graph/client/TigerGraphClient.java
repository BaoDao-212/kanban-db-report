package com.example.graph.client;

import com.example.graph.config.TigerGraphConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "graph.database.type", havingValue = "tigergraph")
@RequiredArgsConstructor
@Slf4j
public class TigerGraphClient {

    private final TigerGraphConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void upsertVertex(String vertexType, String vertexId) throws Exception {
        String url = String.format("%s/graph/%s/vertices/%s/%s",
                config.getBaseUrl(),
                config.getGraphName(),
                vertexType,
                URLEncoder.encode(vertexId, StandardCharsets.UTF_8));

        Map<String, Object> vertex = new HashMap<>();
        vertex.put("id", Map.of("value", vertexId));

        String jsonBody = objectMapper.writeValueAsString(vertex);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("Failed to create vertex: " + response.body());
        }
    }

    public void upsertEdge(String sourceVertexType, String sourceId, String edgeType,
                          String targetVertexType, String targetId, Long relationTypeId) throws Exception {
        String url = String.format("%s/graph/%s/edges/%s/%s/%s/%s/%s",
                config.getBaseUrl(),
                config.getGraphName(),
                sourceVertexType,
                URLEncoder.encode(sourceId, StandardCharsets.UTF_8),
                edgeType,
                targetVertexType,
                URLEncoder.encode(targetId, StandardCharsets.UTF_8));

        Map<String, Object> edge = new HashMap<>();
        edge.put("relationTypeId", Map.of("value", relationTypeId));

        String jsonBody = objectMapper.writeValueAsString(edge);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("Failed to create edge: " + response.body());
        }
    }

    public JsonNode getVertex(String vertexType, String vertexId) throws Exception {
        String url = String.format("%s/graph/%s/vertices/%s/%s",
                config.getBaseUrl(),
                config.getGraphName(),
                vertexType,
                URLEncoder.encode(vertexId, StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            return null;
        }

        return objectMapper.readTree(response.body());
    }

    public List<JsonNode> getAllVertices(String vertexType) throws Exception {
        String url = String.format("%s/graph/%s/vertices/%s",
                config.getBaseUrl(),
                config.getGraphName(),
                vertexType);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            return new ArrayList<>();
        }

        JsonNode root = objectMapper.readTree(response.body());
        List<JsonNode> vertices = new ArrayList<>();

        if (root.has("results")) {
            for (JsonNode result : root.get("results")) {
                vertices.add(result);
            }
        }

        return vertices;
    }

    public void deleteVertex(String vertexType, String vertexId) throws Exception {
        String url = String.format("%s/graph/%s/vertices/%s/%s",
                config.getBaseUrl(),
                config.getGraphName(),
                vertexType,
                URLEncoder.encode(vertexId, StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("Failed to delete vertex: " + response.body());
        }
    }

    public void deleteAllVertices(String vertexType) throws Exception {
        String url = String.format("%s/graph/%s/delete_by_type/vertices/%s",
                config.getBaseUrl(),
                config.getGraphName(),
                vertexType);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("Failed to delete all vertices: " + response.body());
        }
    }

    public JsonNode runQuery(String queryName, Map<String, Object> params) throws Exception {
        String url = String.format("%s/query/%s/%s",
                config.getBaseUrl(),
                config.getGraphName(),
                queryName);

        StringBuilder queryParams = new StringBuilder("?");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            queryParams.append(entry.getKey())
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8))
                    .append("&");
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + queryParams.toString()))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new RuntimeException("Failed to run query: " + response.body());
        }

        return objectMapper.readTree(response.body());
    }

    public long countVertices(String vertexType) throws Exception {
        String url = String.format("%s/builtins/%s/stat/vertex_number?type=%s",
                config.getBaseUrl(),
                config.getGraphName(),
                vertexType);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            return 0;
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (root.has("results") && root.get("results").isArray() && root.get("results").size() > 0) {
            JsonNode result = root.get("results").get(0);
            if (result.has("v_count")) {
                return result.get("v_count").asLong();
            }
        }

        return 0;
    }

    public long countEdges(String edgeType) throws Exception {
        String url = String.format("%s/builtins/%s/stat/edge_number?type=%s",
                config.getBaseUrl(),
                config.getGraphName(),
                edgeType);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            return 0;
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (root.has("results") && root.get("results").isArray() && root.get("results").size() > 0) {
            JsonNode result = root.get("results").get(0);
            if (result.has("e_count")) {
                return result.get("e_count").asLong();
            }
        }

        return 0;
    }
}

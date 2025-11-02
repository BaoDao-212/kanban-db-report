package com.example.graph.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "graph.database.type", havingValue = "tigergraph")
@Getter
public class TigerGraphConfig {

    @Value("${tigergraph.host:localhost}")
    private String host;

    @Value("${tigergraph.port:9000}")
    private int restPort;

    @Value("${tigergraph.graph:MyGraph}")
    private String graphName;

    @Value("${tigergraph.username:tigergraph}")
    private String username;

    @Value("${tigergraph.password:tigergraph}")
    private String password;

    @Value("${tigergraph.token:}")
    private String token;

    public String getBaseUrl() {
        return String.format("http://%s:%d", host, restPort);
    }
}

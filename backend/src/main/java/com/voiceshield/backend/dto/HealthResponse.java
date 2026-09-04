package com.voiceshield.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.Map;

public class HealthResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("service")
    private String service;

    @JsonProperty("version")
    private String version;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("dependencies")
    private Map<String, String> dependencies;

    public HealthResponse() {}

    public HealthResponse(String status, String service, String version, Map<String, String> dependencies) {
        this.status = status;
        this.service = service;
        this.version = version;
        this.timestamp = Instant.now().toString();
        this.dependencies = dependencies;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public Map<String, String> getDependencies() { return dependencies; }
    public void setDependencies(Map<String, String> dependencies) { this.dependencies = dependencies; }
}

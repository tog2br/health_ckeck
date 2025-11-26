package br.com.healthcheck.domain.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Entidade de domínio representando o resultado de um health check
 */
public class HealthCheckResult {
    private String serviceName;
    private String serviceUrl;
    private String category;
    private String status; // healthy, unhealthy, error
    private int statusCode;
    private long responseTime;
    private LocalDateTime timestamp;
    private String message;
    private String rootStatus; // Status raiz do JSON (UP, DOWN, etc.)
    private List<Component> components;
    
    public HealthCheckResult(String serviceName, String serviceUrl, String category) {
        this.serviceName = serviceName;
        this.serviceUrl = serviceUrl;
        this.category = category != null ? category : "Geral";
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters e Setters
    public String getServiceName() { return serviceName; }
    public String getServiceUrl() { return serviceUrl; }
    public String getCategory() { return category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }
    public long getResponseTime() { return responseTime; }
    public void setResponseTime(long responseTime) { this.responseTime = responseTime; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRootStatus() { return rootStatus; }
    public void setRootStatus(String rootStatus) { this.rootStatus = rootStatus; }
    public List<Component> getComponents() { return components; }
    public void setComponents(List<Component> components) { this.components = components; }
}


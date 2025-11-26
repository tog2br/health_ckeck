package br.com.healthcheck.presentation.dto;

import br.com.healthcheck.domain.entity.HealthCheckResult;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DTO de resposta para o endpoint de health check
 */
public class HealthCheckResponse {
    private Map<String, List<ServiceHealthDto>> services;
    private SummaryDto summary;
    
    public HealthCheckResponse(List<HealthCheckResult> results) {
        // Agrupar por categoria
        this.services = results.stream()
            .collect(Collectors.groupingBy(
                HealthCheckResult::getCategory,
                LinkedHashMap::new,
                Collectors.mapping(ServiceHealthDto::from, Collectors.toList())
            ));
        
        // Calcular estatísticas
        int total = results.size();
        int healthy = (int) results.stream().filter(r -> "healthy".equals(r.getStatus())).count();
        int unhealthy = (int) results.stream().filter(r -> "unhealthy".equals(r.getStatus())).count();
        int errors = (int) results.stream().filter(r -> "error".equals(r.getStatus())).count();
        
        this.summary = new SummaryDto(total, healthy, unhealthy, errors);
    }
    
    public Map<String, List<ServiceHealthDto>> getServices() {
        return services;
    }
    
    public SummaryDto getSummary() {
        return summary;
    }
    
    public static class ServiceHealthDto {
        private String name;
        private String url;
        private String category;
        private String status;
        private int statusCode;
        private long responseTime;
        private String timestamp;
        private String message;
        private HealthDetailsDto healthDetails;
        
        public static ServiceHealthDto from(HealthCheckResult result) {
            ServiceHealthDto dto = new ServiceHealthDto();
            dto.name = result.getServiceName();
            dto.url = result.getServiceUrl();
            dto.category = result.getCategory();
            dto.status = result.getStatus();
            dto.statusCode = result.getStatusCode();
            dto.responseTime = result.getResponseTime();
            dto.timestamp = result.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            dto.message = result.getMessage();
            
            if (result.getRootStatus() != null || 
                (result.getComponents() != null && !result.getComponents().isEmpty())) {
                dto.healthDetails = new HealthDetailsDto();
                dto.healthDetails.rootStatus = result.getRootStatus();
                if (result.getComponents() != null) {
                    dto.healthDetails.components = result.getComponents().stream()
                        .map(c -> new ComponentDto(c.getName(), c.getStatus()))
                        .collect(Collectors.toList());
                }
            }
            
            return dto;
        }
        
        // Getters
        public String getName() { return name; }
        public String getUrl() { return url; }
        public String getCategory() { return category; }
        public String getStatus() { return status; }
        public int getStatusCode() { return statusCode; }
        public long getResponseTime() { return responseTime; }
        public String getTimestamp() { return timestamp; }
        public String getMessage() { return message; }
        public HealthDetailsDto getHealthDetails() { return healthDetails; }
    }
    
    public static class HealthDetailsDto {
        private String rootStatus;
        private List<ComponentDto> components;
        
        public String getRootStatus() { return rootStatus; }
        public List<ComponentDto> getComponents() { return components; }
    }
    
    public static class ComponentDto {
        private String name;
        private String status;
        
        public ComponentDto(String name, String status) {
            this.name = name;
            this.status = status;
        }
        
        public String getName() { return name; }
        public String getStatus() { return status; }
    }
    
    public static class SummaryDto {
        private int total;
        private int healthy;
        private int unhealthy;
        private int errors;
        private String timestamp;
        
        public SummaryDto(int total, int healthy, int unhealthy, int errors) {
            this.total = total;
            this.healthy = healthy;
            this.unhealthy = unhealthy;
            this.errors = errors;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
        
        public int getTotal() { return total; }
        public int getHealthy() { return healthy; }
        public int getUnhealthy() { return unhealthy; }
        public int getErrors() { return errors; }
        public String getTimestamp() { return timestamp; }
    }
}


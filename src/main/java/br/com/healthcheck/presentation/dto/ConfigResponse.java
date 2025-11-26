package br.com.healthcheck.presentation.dto;

import br.com.healthcheck.domain.entity.Service;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO de resposta para o endpoint de configuração
 */
public class ConfigResponse {
    private int refreshInterval;
    private int timeout;
    private List<ServiceDto> services;
    private String environment;
    
    public ConfigResponse(int refreshInterval, int timeout, List<Service> services, String environment) {
        this.refreshInterval = refreshInterval;
        this.timeout = timeout;
        this.services = services.stream()
            .map(s -> new ServiceDto(s.getName(), s.getUrl(), s.getCategory(), s.getExpectedStatus()))
            .collect(Collectors.toList());
        this.environment = environment;
    }
    
    public int getRefreshInterval() { return refreshInterval; }
    public int getTimeout() { return timeout; }
    public List<ServiceDto> getServices() { return services; }
    public String getEnvironment() { return environment; }
    
    public static class ServiceDto {
        private String name;
        private String url;
        private String category;
        private int expectedStatus;
        
        public ServiceDto(String name, String url, String category, int expectedStatus) {
            this.name = name;
            this.url = url;
            this.category = category;
            this.expectedStatus = expectedStatus;
        }
        
        public String getName() { return name; }
        public String getUrl() { return url; }
        public String getCategory() { return category; }
        public int getExpectedStatus() { return expectedStatus; }
    }
}


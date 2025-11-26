package br.com.healthcheck.domain.usecase;

import br.com.healthcheck.domain.entity.Service;
import br.com.healthcheck.domain.repository.ConfigRepository;
import java.util.List;

/**
 * Caso de uso: Obter configuração
 */
public class GetConfigUseCase {
    private final ConfigRepository configRepository;
    
    public GetConfigUseCase(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }
    
    public ConfigResult execute() {
        return new ConfigResult(
            configRepository.getServices(),
            configRepository.getRefreshInterval(),
            configRepository.getTimeout()
        );
    }
    
    public static class ConfigResult {
        private final List<Service> services;
        private final int refreshInterval;
        private final int timeout;
        
        public ConfigResult(List<Service> services, int refreshInterval, int timeout) {
            this.services = services;
            this.refreshInterval = refreshInterval;
            this.timeout = timeout;
        }
        
        public List<Service> getServices() { return services; }
        public int getRefreshInterval() { return refreshInterval; }
        public int getTimeout() { return timeout; }
    }
}


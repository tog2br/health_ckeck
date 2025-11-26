package br.com.healthcheck.domain.usecase;

import br.com.healthcheck.domain.entity.HealthCheckResult;
import br.com.healthcheck.domain.entity.Service;
import br.com.healthcheck.domain.repository.ConfigRepository;
import br.com.healthcheck.domain.repository.HealthCheckRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Caso de uso: Verificar saúde de todos os serviços
 */
public class CheckHealthUseCase {
    private final HealthCheckRepository healthCheckRepository;
    private final ConfigRepository configRepository;
    
    public CheckHealthUseCase(HealthCheckRepository healthCheckRepository, 
                             ConfigRepository configRepository) {
        this.healthCheckRepository = healthCheckRepository;
        this.configRepository = configRepository;
    }
    
    public HealthCheckSummary execute() {
        List<Service> services = configRepository.getServices();
        int timeout = configRepository.getTimeout();
        
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Future<HealthCheckResult>> futures = new ArrayList<>();
        
        for (Service service : services) {
            futures.add(executor.submit(() -> 
                healthCheckRepository.checkHealth(service, timeout)
            ));
        }
        
        List<HealthCheckResult> results = new ArrayList<>();
        for (Future<HealthCheckResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
        
        return new HealthCheckSummary(results);
    }
    
    public static class HealthCheckSummary {
        private final List<HealthCheckResult> results;
        
        public HealthCheckSummary(List<HealthCheckResult> results) {
            this.results = results;
        }
        
        public List<HealthCheckResult> getResults() {
            return results;
        }
        
        public int getTotal() {
            return results.size();
        }
        
        public int getHealthy() {
            return (int) results.stream().filter(r -> "healthy".equals(r.getStatus())).count();
        }
        
        public int getUnhealthy() {
            return (int) results.stream().filter(r -> "unhealthy".equals(r.getStatus())).count();
        }
        
        public int getErrors() {
            return (int) results.stream().filter(r -> "error".equals(r.getStatus())).count();
        }
    }
}


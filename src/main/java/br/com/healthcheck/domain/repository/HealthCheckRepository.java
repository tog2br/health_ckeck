package br.com.healthcheck.domain.repository;

import br.com.healthcheck.domain.entity.HealthCheckResult;
import br.com.healthcheck.domain.entity.Service;

/**
 * Interface do repositório de health check (Domain Layer)
 */
public interface HealthCheckRepository {
    HealthCheckResult checkHealth(Service service, int timeout);
}


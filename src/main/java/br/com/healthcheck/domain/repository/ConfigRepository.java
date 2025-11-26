package br.com.healthcheck.domain.repository;

import br.com.healthcheck.domain.entity.Service;
import java.util.List;

/**
 * Interface do repositório de configuração (Domain Layer)
 */
public interface ConfigRepository {
    List<Service> getServices();
    int getRefreshInterval();
    int getTimeout();
    void saveConfig(List<Service> services, int refreshInterval, int timeout);
}


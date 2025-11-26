package br.com.healthcheck.domain.usecase;

import br.com.healthcheck.domain.entity.Service;
import br.com.healthcheck.domain.repository.ConfigRepository;
import java.util.List;

/**
 * Caso de uso: Salvar configuração
 */
public class SaveConfigUseCase {
    private final ConfigRepository configRepository;
    
    public SaveConfigUseCase(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }
    
    public void execute(List<Service> services, int refreshInterval, int timeout) {
        configRepository.saveConfig(services, refreshInterval, timeout);
    }
}


package br.com.healthcheck.infrastructure.server;

import br.com.healthcheck.data.repository.HttpHealthCheckRepository;
import br.com.healthcheck.data.repository.JsonConfigRepository;
import br.com.healthcheck.domain.repository.ConfigRepository;
import br.com.healthcheck.domain.repository.HealthCheckRepository;
import br.com.healthcheck.domain.usecase.CheckHealthUseCase;
import br.com.healthcheck.domain.usecase.GetConfigUseCase;
import br.com.healthcheck.domain.usecase.SaveConfigUseCase;
import br.com.healthcheck.infrastructure.config.EnvironmentManager;
import br.com.healthcheck.presentation.handler.ConfigHandler;
import br.com.healthcheck.presentation.handler.EnvironmentHandler;
import br.com.healthcheck.presentation.handler.HealthHandler;
import br.com.healthcheck.presentation.handler.StaticFileHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Servidor principal da aplicação Health Check
 * Usa Clean Architecture com separação de responsabilidades
 */
public class HealthCheckServer {
    private static final int PORT = 3000;
    
    public static void main(String[] args) {
        try {
            // Configuração de dependências (Dependency Injection manual)
            String configFile = EnvironmentManager.getConfigFile();
            ConfigRepository configRepository = new JsonConfigRepository(configFile);
            HealthCheckRepository healthCheckRepository = new HttpHealthCheckRepository();
            
            // Use Cases
            CheckHealthUseCase checkHealthUseCase = new CheckHealthUseCase(
                healthCheckRepository, 
                configRepository
            );
            GetConfigUseCase getConfigUseCase = new GetConfigUseCase(configRepository);
            SaveConfigUseCase saveConfigUseCase = new SaveConfigUseCase(configRepository);
            
            // Handlers HTTP
            HealthHandler healthHandler = new HealthHandler(checkHealthUseCase);
            ConfigHandler configHandler = new ConfigHandler(getConfigUseCase, saveConfigUseCase);
            EnvironmentHandler environmentHandler = new EnvironmentHandler();
            StaticFileHandler staticFileHandler = new StaticFileHandler();
            
            // Criar e configurar servidor HTTP
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            
            server.createContext("/", staticFileHandler);
            server.createContext("/index.html", staticFileHandler);
            server.createContext("/api/health", healthHandler);
            server.createContext("/api/config", configHandler);
            server.createContext("/api/environment", environmentHandler);
            
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            
            System.out.println("🚀 Servidor Health Check rodando em http://localhost:" + PORT);
            System.out.println("📊 Dashboard disponível em http://localhost:" + PORT);
            System.out.println("🌍 Ambiente inicial: " + EnvironmentManager.getEnvironment());
            System.out.println("📝 Pressione Ctrl+C para parar o servidor");
            
        } catch (IOException e) {
            System.err.println("❌ Erro ao iniciar servidor: " + e.getMessage());
            if (e.getMessage().contains("Address already in use")) {
                System.err.println("   A porta " + PORT + " já está em uso.");
            }
            System.exit(1);
        }
    }
}


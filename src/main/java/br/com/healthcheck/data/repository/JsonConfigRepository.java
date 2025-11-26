package br.com.healthcheck.data.repository;

import br.com.healthcheck.domain.entity.Service;
import br.com.healthcheck.domain.repository.ConfigRepository;
import br.com.healthcheck.infrastructure.util.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação do repositório de configuração usando arquivo JSON
 */
public class JsonConfigRepository implements ConfigRepository {
    private final String configFilePath;
    
    public JsonConfigRepository(String configFilePath) {
        this.configFilePath = configFilePath;
    }
    
    @Override
    public List<Service> getServices() {
        try {
            if (!Files.exists(Paths.get(configFilePath))) {
                return new ArrayList<>();
            }
            
            String content = new String(
                Files.readAllBytes(Paths.get(configFilePath)), 
                StandardCharsets.UTF_8
            );
            
            JsonParser.JsonObject config = JsonParser.JsonObject.parse(content);
            JsonParser.JsonArray servicesArray = config.getArray("services");
            List<Service> services = new ArrayList<>();
            
            for (int i = 0; i < servicesArray.size(); i++) {
                JsonParser.JsonObject serviceObj = servicesArray.getObject(i);
                services.add(new Service(
                    serviceObj.getString("name"),
                    serviceObj.getString("url"),
                    serviceObj.getString("category", "Geral"),
                    serviceObj.getInt("expectedStatus", 200)
                ));
            }
            
            return services;
        } catch (Exception e) {
            System.err.println("Erro ao ler configuração: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public int getRefreshInterval() {
        try {
            String content = new String(
                Files.readAllBytes(Paths.get(configFilePath)), 
                StandardCharsets.UTF_8
            );
            JsonParser.JsonObject config = JsonParser.JsonObject.parse(content);
            return config.getInt("refreshInterval", 30000);
        } catch (Exception e) {
            return 30000;
        }
    }
    
    @Override
    public int getTimeout() {
        try {
            String content = new String(
                Files.readAllBytes(Paths.get(configFilePath)), 
                StandardCharsets.UTF_8
            );
            JsonParser.JsonObject config = JsonParser.JsonObject.parse(content);
            return config.getInt("timeout", 5000);
        } catch (Exception e) {
            return 5000;
        }
    }
    
    @Override
    public void saveConfig(List<Service> services, int refreshInterval, int timeout) {
        try {
            JsonParser.JsonObject config = new JsonParser.JsonObject();
            config.put("refreshInterval", refreshInterval);
            config.put("timeout", timeout);
            
            JsonParser.JsonArray servicesArray = new JsonParser.JsonArray();
            for (Service service : services) {
                JsonParser.JsonObject serviceObj = new JsonParser.JsonObject();
                serviceObj.put("name", service.getName());
                serviceObj.put("url", service.getUrl());
                serviceObj.put("category", service.getCategory());
                serviceObj.put("expectedStatus", service.getExpectedStatus());
                servicesArray.add(serviceObj);
            }
            
            config.put("services", servicesArray);
            
            Files.write(
                Paths.get(configFilePath),
                config.toJsonString().getBytes(StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar configuração", e);
        }
    }
}


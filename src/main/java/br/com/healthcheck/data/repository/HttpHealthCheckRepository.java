package br.com.healthcheck.data.repository;

import br.com.healthcheck.domain.entity.Component;
import br.com.healthcheck.domain.entity.HealthCheckResult;
import br.com.healthcheck.domain.entity.Service;
import br.com.healthcheck.domain.repository.HealthCheckRepository;
import br.com.healthcheck.infrastructure.util.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação do repositório de health check usando HTTP
 */
public class HttpHealthCheckRepository implements HealthCheckRepository {
    
    @Override
    public HealthCheckResult checkHealth(Service service, int timeout) {
        long startTime = System.currentTimeMillis();
        HealthCheckResult result = new HealthCheckResult(
            service.getName(),
            service.getUrl(),
            service.getCategory()
        );
        
        try {
            URL urlObj = new URL(service.getUrl());
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setRequestProperty("User-Agent", "HealthCheck/1.0");
            conn.setRequestProperty("Accept", "application/json");
            
            int statusCode = conn.getResponseCode();
            long responseTime = System.currentTimeMillis() - startTime;
            
            boolean isHealthy = statusCode == service.getExpectedStatus();
            result.setStatusCode(statusCode);
            result.setResponseTime(responseTime);
            
            // Tentar ler o corpo da resposta se for JSON
            if (statusCode >= 200 && statusCode < 300) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder responseBody = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBody.append(line);
                    }
                    
                    String body = responseBody.toString().trim();
                    if (body.startsWith("{") && body.endsWith("}")) {
                        parseHealthCheckJson(body, result);
                    }
                } catch (Exception e) {
                    // Ignora erros ao ler o corpo
                }
            }
            
            // Verificar status raiz se existir
            if (result.getRootStatus() != null && !"UP".equalsIgnoreCase(result.getRootStatus())) {
                isHealthy = false;
            }
            
            // Verificar se algum componente está diferente de UP
            if (result.getComponents() != null && !result.getComponents().isEmpty()) {
                for (Component component : result.getComponents()) {
                    String componentStatus = component.getStatus();
                    if (componentStatus != null && !"UP".equalsIgnoreCase(componentStatus)) {
                        isHealthy = false;
                        break; // Já encontrou um componente com problema, não precisa continuar
                    }
                }
            }
            
            result.setStatus(isHealthy ? "healthy" : "unhealthy");
            result.setMessage(isHealthy ? "Operacional" : "Status " + statusCode);
            
        } catch (SocketTimeoutException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            result.setStatus("error");
            result.setStatusCode(0);
            result.setResponseTime(responseTime);
            result.setMessage("Timeout");
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            result.setStatus("error");
            result.setStatusCode(0);
            result.setResponseTime(responseTime);
            String msg = e.getMessage();
            result.setMessage(msg != null ? msg : "Erro de conexão");
        }
        
        return result;
    }
    
    private void parseHealthCheckJson(String json, HealthCheckResult result) {
        try {
            JsonParser.JsonObject healthJson = JsonParser.JsonObject.parse(json);
            
            // Verificar status raiz
            String rootStatus = healthJson.getString("status", "");
            if (!rootStatus.isEmpty()) {
                result.setRootStatus(rootStatus);
            }
            
            // Verificar componentes
            Object componentsObj = healthJson.get("components");
            if (componentsObj instanceof JsonParser.JsonObject) {
                JsonParser.JsonObject componentsJson = (JsonParser.JsonObject) componentsObj;
                List<Component> components = extractComponents(componentsJson, "");
                result.setComponents(components);
            }
        } catch (Exception e) {
            // Se não conseguir parsear, ignora
        }
    }
    
    private List<Component> extractComponents(JsonParser.JsonObject components, String prefix) {
        List<Component> result = new ArrayList<>();
        
        for (String key : components.keySet()) {
            Object value = components.get(key);
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            
            if (value instanceof JsonParser.JsonObject) {
                JsonParser.JsonObject compObj = (JsonParser.JsonObject) value;
                String status = compObj.getString("status", "");
                
                if (!status.isEmpty()) {
                    result.add(new Component(fullKey, status));
                }
                
                // Verificar se tem sub-componentes
                Object subComponents = compObj.get("components");
                if (subComponents instanceof JsonParser.JsonObject) {
                    result.addAll(extractComponents((JsonParser.JsonObject) subComponents, fullKey));
                }
            }
        }
        
        return result;
    }
}


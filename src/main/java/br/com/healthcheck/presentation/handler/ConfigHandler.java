package br.com.healthcheck.presentation.handler;

import br.com.healthcheck.domain.entity.Service;
import br.com.healthcheck.domain.usecase.GetConfigUseCase;
import br.com.healthcheck.domain.usecase.SaveConfigUseCase;
import br.com.healthcheck.infrastructure.config.EnvironmentManager;
import br.com.healthcheck.infrastructure.util.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler HTTP para o endpoint /api/config
 */
public class ConfigHandler implements HttpHandler {
    private final GetConfigUseCase getConfigUseCase;
    private final SaveConfigUseCase saveConfigUseCase;
    
    public ConfigHandler(GetConfigUseCase getConfigUseCase, SaveConfigUseCase saveConfigUseCase) {
        this.getConfigUseCase = getConfigUseCase;
        this.saveConfigUseCase = saveConfigUseCase;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("GET")) {
            handleGet(exchange);
        } else if (exchange.getRequestMethod().equals("POST")) {
            handlePost(exchange);
        } else {
            sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
        }
    }
    
    private void handleGet(HttpExchange exchange) throws IOException {
        try {
            GetConfigUseCase.ConfigResult config = getConfigUseCase.execute();
            
            JsonParser.JsonObject json = new JsonParser.JsonObject();
            json.put("refreshInterval", config.getRefreshInterval());
            json.put("timeout", config.getTimeout());
            json.put("environment", EnvironmentManager.getEnvironment());
            
            JsonParser.JsonArray servicesArray = new JsonParser.JsonArray();
            for (Service service : config.getServices()) {
                JsonParser.JsonObject serviceJson = new JsonParser.JsonObject();
                serviceJson.put("name", service.getName());
                serviceJson.put("url", service.getUrl());
                serviceJson.put("category", service.getCategory());
                serviceJson.put("expectedStatus", service.getExpectedStatus());
                servicesArray.add(serviceJson);
            }
            json.put("services", servicesArray);
            
            sendResponse(exchange, 200, "application/json; charset=utf-8", json.toJsonString());
            
        } catch (Exception e) {
            sendResponse(exchange, 500, "application/json", 
                "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    private void handlePost(HttpExchange exchange) throws IOException {
        try {
            String requestBody = readRequestBody(exchange);
            JsonParser.JsonObject request = JsonParser.JsonObject.parse(requestBody);
            
            int refreshInterval = request.getInt("refreshInterval", 30000);
            int timeout = request.getInt("timeout", 5000);
            JsonParser.JsonArray servicesArray = request.getArray("services");
            
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
            
            saveConfigUseCase.execute(services, refreshInterval, timeout);
            
            JsonParser.JsonObject response = new JsonParser.JsonObject();
            response.put("success", true);
            response.put("message", "Configuração atualizada com sucesso");
            response.put("environment", EnvironmentManager.getEnvironment());
            
            sendResponse(exchange, 200, "application/json; charset=utf-8", response.toJsonString());
            
        } catch (Exception e) {
            sendResponse(exchange, 500, "application/json", 
                "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }
    
    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(line);
            }
            return sb.toString();
        }
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, 
                             String contentType, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        
        try (java.io.OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        exchange.close();
    }
    
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}

